package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.LineSeparator
import io.netty.resolver.DefaultAddressResolverGroup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import java.net.InetSocketAddress
import java.util.function.Function


class FafLobbyClient(
  private val mapper: ObjectMapper,
) : FafLobbyApi {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(FafLobbyApi::class.java)
  }

  private lateinit var config: Config
  private lateinit var outboundSink: Sinks.Many<ClientMessage>
  private var connection: Connection? = null

  data class Config(
    val token: String,
    val version: String,
    val userAgent: String,
    val host: String,
    val port: Int,
    val generateUid: Function<Long, String>,
    val bufferSize: Int,
    val wiretap: Boolean = false,
  )

  private val eventSink: Sinks.Many<ServerMessage> = Sinks.many().multicast().directBestEffort()
  private val disconnectsSink: Sinks.Many<Unit> = Sinks.many().multicast().directBestEffort()
  private val rawEvents = eventSink.asFlux()

  override val events = eventSink.asFlux().filter {
    it !is PingMessage &&
      it !is PongMessage &&
      it !is SessionResponse &&
      it !is LoginSuccessResponse &&
      it !is LoginFailedResponse
  }

  override val disconnects = disconnectsSink.asFlux();

  private val client = TcpClient.newConnection()
    .resolver(DefaultAddressResolverGroup.INSTANCE)
    .doOnResolveError { connection, throwable ->
      LOG.error("Could not connect to server", throwable)
      connection.dispose()
    }
    .doOnConnected {
      val address = it.channel().remoteAddress() as InetSocketAddress
      LOG.info("Connected to {} on port {}", address.hostName, address.port)
      it.addHandler(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
        .addHandler(LineBasedFrameDecoder(config.bufferSize))
    }
    .doOnDisconnected {
      LOG.info("Disconnected from server")
      it.dispose()
      disconnectsSink.tryEmitNext(Unit)
    }

  private fun openConnection(): Mono<out Connection> {
    outboundSink = Sinks.many().unicast().onBackpressureBuffer()
    return client
      .wiretap(config.wiretap)
      .host(config.host)
      .port(config.port)
      .handle { inbound, outbound ->
        val inboundMono = inbound.receive()
          .asString(Charsets.UTF_8)
          .doOnError { LOG.error("Inbound channel closed with error", it) }
          .doOnComplete { LOG.info("Inbound channel closed") }
          .doOnCancel { LOG.info("Inbound channel cancelled") }
          .flatMap {
            Mono.fromCallable {
              val serverMessage = mapper.readValue(it, ServerMessage::class.java)
              var logMessage = it
              for (string in serverMessage.stringsToMask()) {
                logMessage = logMessage.replace(string, LobbyProtocolMessage.CONFIDENTIAL_MASK)
              }
              LOG.debug("Inbound message: {}", logMessage)
              serverMessage
            }.onErrorResume { throwable ->
              LOG.error("Error during deserialization of message {}", it, throwable)
              Mono.empty()
            }
          }
          .flatMap { message ->
            handle(message)
              .onErrorResume {
                LOG.error("Error during handling of message {}", message, it)
                Mono.empty()
              }
          }
          .then()

        val outboundMono = outbound.sendString(
          outboundSink.asFlux()
            .doOnError { LOG.error("Outbound channel closed with error", it) }
            .doOnComplete { LOG.info("Outbound channel closed") }
            .doOnCancel { LOG.info("Outbound channel cancelled") }
            // appending line ending is workaround due to broken encoder
            .flatMap {
              Mono.fromCallable {
                val jsonMessage = mapper.writeValueAsString(it)
                var logMessage = jsonMessage
                for (string in it.stringsToMask()) {
                  logMessage = logMessage.replace(string, LobbyProtocolMessage.CONFIDENTIAL_MASK)
                }
                LOG.debug("Outbound message: {}", logMessage)
                jsonMessage + "\n"
              }.onErrorResume { throwable ->
                LOG.error("Error during serialization of message {}", it, throwable)
                Mono.empty()
              }
            }
        ).then()

        /* The lobby protocol requires two-way communication. If either the outbound or inbound connections complete/close
           then we are better off closing the connection to the server. This is why we return a mono that completes when one
           of the connections finishes */
        Mono.firstWithSignal(inboundMono, outboundMono)
      }
      .connect()
  }

  override fun connectAndLogin(config: Config): Mono<LoginSuccessResponse> {
    this.config = config
    return openConnection()
      .doOnSuccess { connection = it }
      .doOnError { LOG.error("Error during connection", it) }
      .then(Mono.fromCallable { send(SessionRequest(config.version, config.userAgent)) })
      .then(rawEvents
        .flatMap {
          when (it) {
            is LoginSuccessResponse -> Mono.just(it)
            is LoginFailedResponse -> Mono.error(LoginException(it.text))
            else -> Mono.empty()
          }
        }
        .cast(LoginSuccessResponse::class.java)
        .next())
  }

  override fun disconnect() {
    when (val conn = connection) {
      null -> LOG.warn("Attempting to disconnect while never connected")
      else -> LOG.info("Disconnecting from server").also {
        outboundSink.tryEmitComplete()
        conn.dispose()
      }
    }
  }

  private fun send(message: ClientMessage) {
    outboundSink.tryEmitNext(message)
  }

  private fun handle(message: ServerMessage): Mono<Unit> = when (message) {
    is SessionResponse -> Mono.fromCallable {
      send(
        AuthenticateRequest(
          config.token,
          message.session,
          config.generateUid.apply(message.session),
        )
      )
    }
    else -> Mono.fromCallable {
      eventSink.tryEmitNext(message)
      Unit
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
    ratingMin: Int?,
    ratingMax: Int?,
    enforceRatingRange: Boolean
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
          ratingMin,
          ratingMax,
          enforceRatingRange,
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

  override fun getIceServers(): Flux<IceServer> =
    Mono.fromCallable { send(IceServerListRequest()) }
      .thenMany(
        events
          .filter { it is IceServerListResponse }
          .cast(IceServerListResponse::class.java)
          .next()
          .flatMapIterable { it.iceServers }
      )

  override fun addFriend(playerId: Int) = send(AddFriendRequest(playerId))

  override fun addFoe(playerId: Int) = send(AddFoeRequest(playerId))

  override fun removeFriend(playerId: Int) = send(RemoveFriendRequest(playerId))

  override fun removeFoe(playerId: Int) = send(RemoveFoeRequest(playerId))

  override fun selectAvatar(url: String?) = send(SelectAvatarRequest(url))

  override fun getAvailableAvatars(): Flux<Player.Avatar> = Mono.fromCallable { send(AvatarListRequest()) }
    .thenMany(
      events
        .filter { it is AvatarListInfo }
        .cast(AvatarListInfo::class.java)
        .next()
        .flatMapIterable { it.avatarList }
    )

  override fun requestMatchmakerInfo() = send(MatchmakerInfoRequest())

  override fun gameMatchmaking(queueName: String, state: MatchmakerState) =
    send(GameMatchmakingRequest(queueName, state))

  override fun inviteToParty(playerId: Int) = send(InviteToPartyRequest(playerId))

  override fun acceptPartyInvite(playerId: Int) = send(AcceptInviteToPartyRequest(playerId))

  override fun kickPlayerFromParty(playerId: Int) = send(KickPlayerFromPartyRequest(playerId))

  override fun readyParty() = send(ReadyPartyRequest())

  override fun unreadyParty() = send(UnreadyPartyRequest())

  override fun leaveParty() = send(LeavePartyRequest())

  override fun setPartyFactions(factions: Set<Faction>) = send(SelectPartyFactionsRequest(factions))

  override fun sendGpgGameMessage(message: GpgGameOutboundMessage) = send(message)
}
