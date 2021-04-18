package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.buffer.Unpooled
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.LineSeparator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import java.util.function.Function


class FafLobbyClient(
  private val config: Config,
  private val mapper: ObjectMapper,
) : FafLobbyApi {
  companion object {
    val log: Logger = LoggerFactory.getLogger(FafLobbyApi::class.java)
  }

  data class Config(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val localIp: String,
    val generateUid: Function<Long, String>,
    val bufferSize: Int,
    val wiretap: Boolean = false,
  )

  private val eventSink: Sinks.Many<ServerMessage> = Sinks.many().multicast().directBestEffort()
  private val rawEvents = eventSink.asFlux()

  override val events = eventSink.asFlux().filter {
    it !is PingMessage &&
      it !is SessionResponse &&
      it !is LoginSuccessResponse &&
      it !is LoginFailedResponse
  }

  private val outboundSink: Sinks.Many<String> = Sinks.many().unicast().onBackpressureBuffer()
  private val connectionMono: Mono<out Connection> = TcpClient.create(
  )
    .wiretap(config.wiretap)
    .host(config.host)
    .port(config.port)
    .doOnConnected { connection ->
      connection
        .addHandler(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
        .addHandler(LineBasedFrameDecoder(config.bufferSize))
    }
    .handle { inbound, outbound ->
      val inboundMono = inbound.receive()
        .asString(Charsets.UTF_8)
        .doOnNext { log.debug("Inbound message: {}", it) }
        .map { mapper.readValue(it, ServerMessage::class.java) }
        .flatMap { handle(it) }
        .onErrorResume {
          log.error("Error during read", it)
          // Show must go on!
          Mono.empty()
        }
        .doOnComplete { log.info("Inbound channel closed") }
        .doFinally { log.info("Inbound channel finally") }
        .then()

      val outboundMono = outbound.send(
        outboundSink.asFlux()
          .doOnNext { log.debug("Outbound message: {}", it) }
          .onErrorResume {
            log.error("Error during write", it)
            // Show must go on!
            Mono.empty()
          }
          .doOnComplete { log.error("Outbound channel closed") }
          .doFinally { log.info("Outbound channel finally") }
          // appending line ending is workaround due to broken encoder
          .map { message -> Unpooled.copiedBuffer(message + "\n", Charsets.UTF_8) }
      ).then()

      inboundMono.mergeWith(outboundMono)
    }
    .connect()

  private var connectionSubscription: Disposable? = null

  override fun connectAndLogin() =
    Mono.fromCallable {
      connectionSubscription = connectionMono.subscribe()
      send(SessionRequest())
    }.then(
      rawEvents
        .flatMap {
          when (it) {
            is LoginSuccessResponse -> Mono.just(it)
            is LoginFailedResponse -> Mono.error(LoginException(it.text))
            else -> Mono.empty()
          }
        }
        .cast(LoginSuccessResponse::class.java)
        .next()
    )

  override fun disconnect() {
    when (val subscription = connectionSubscription) {
      null -> log.warn("Attempting to disconnect while never connected")
      else -> subscription.dispose().also { log.info("Disconnecting") }
    }
  }

  override fun broadcastMessage(message: String) = send(BroadcastRequest(message))

  override fun closePlayerGame(playerId: Int) = send(ClosePlayerGameRequest(playerId))

  override fun closePlayerLobby(playerId: Int) = send(ClosePlayerLobbyRequest(playerId))

  override fun requestHostGame(
    title: String,
    mapName: String,
    mod: String,
    visibility: GameVisibility,
    password: String?,
  ): Mono<GameLaunchResponse> =
    Mono.fromCallable {
      send(
        HostGameRequest(
          mapName,
          title,
          mod,
          BooleanArray(0),
          if (password == null) GameAccess.PUBLIC else GameAccess.PASSWORD,
          0,
          password,
          visibility,
        )
      )
    }.then(
      events
        .filter { it is GameLaunchResponse }
        .cast(GameLaunchResponse::class.java)
        .next()
    )

  override fun requestJoinGame(gameId: Int, password: String?) =
    Mono.fromCallable {
      send(JoinGameRequest(gameId, password))
    }.then(
      events
        .filter { it is GameLaunchResponse }
        .cast(GameLaunchResponse::class.java)
        .next()
    )

  override fun restoreGameSession(gameId: Int) = send(RestoreGameSessionRequest(gameId))

  override fun getIceServers(): Mono<Collection<IceServer>> =
    Mono.fromCallable { send(IceServerListRequest()) }
      .then(
        events
          .filter { it is IceServerListResponse }
          .cast(IceServerListResponse::class.java)
          .next()
          .map { it.iceServers }
      )

  private fun send(message: ClientMessage) {
    outboundSink.tryEmitNext(mapper.writeValueAsString(message))
  }

  private fun handle(message: ServerMessage): Mono<Unit> = when (message) {
    is SessionResponse -> Mono.fromCallable {
      send(
        LoginRequest(
          config.username,
          config.password,
          message.session,
          config.localIp,
          config.generateUid.apply(message.session),
        )
      )
      eventSink.tryEmitNext(message)
      Unit
    }
    else -> Mono.fromCallable {
      eventSink.tryEmitNext(message)
      Unit
    }
  }

  override fun addFriend(playerId: Int) = send(AddFriendRequest(playerId))

  override fun addFoe(playerId: Int) = send(AddFoeRequest(playerId))

  override fun removeFriend(playerId: Int) = send(RemoveFriendRequest(playerId))

  override fun removeFoe(playerId: Int) = send(RemoveFoeRequest(playerId))

  override fun requestMatchmakerInfo() = send(MatchmakerInfoRequest())

  override fun gameMatchmaking(queueName: String, state: MatchmakerState) =
    send(GameMatchmakingRequest(queueName, state))

  override fun inviteToParty(playerId: Int) = send(InviteToPartyRequest(playerId))

  override fun acceptPartyInvite(playerId: Int) = send(AcceptInviteToPartyRequest(playerId))

  override fun kickPlayerFromParty(playerId: Int) = send(KickPlayerFromPartyRequest(playerId))

  override fun readyParty(isReady: Boolean) = send(ReadyPartyRequest(isReady))

  override fun leaveParty() = send(LeavePartyRequest())

  override fun setPartyFactions(factions: Set<Faction>) = send(SelectPartyFactionsRequest(factions))

  override fun sendGpgGameMessage(message: GpgGameOutboundMessage) = send(message)
}
