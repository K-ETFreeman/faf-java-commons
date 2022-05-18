package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.LineSeparator
import io.netty.resolver.DefaultAddressResolverGroup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.EmitFailureHandler
import reactor.core.publisher.Sinks.EmitResult
import reactor.core.scheduler.Schedulers
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import reactor.util.retry.Retry
import reactor.util.retry.Retry.RetrySignal
import java.net.InetSocketAddress
import java.time.Duration
import java.util.function.Function


class FafLobbyClient(
  private val mapper: ObjectMapper,
) : FafLobbyApi {
  companion object {
    val LOG: Logger = LoggerFactory.getLogger(FafLobbyApi::class.java)
  }

  data class Config(
    val tokenMono: Mono<String>,
    val version: String,
    val userAgent: String,
    val host: String,
    val port: Int,
    val generateUid: Function<Long, String>,
    val bufferSize: Int,
    val wiretap: Boolean = false,
    val maxRetryAttempts: Long,
    val retryWaitSeconds: Long,
  )

  private lateinit var config: Config
  private lateinit var outboundSink: Sinks.Many<ClientMessage>
  private lateinit var outboundMessages: Flux<ClientMessage>

  private var connection: Connection? = null
  private var pingDisposable: Disposable? = null
  private var loginMono: Mono<LoginSuccessResponse>? = null
  private var connecting: Boolean = false

  var minPingIntervalSeconds: Long = 60
  var autoReconnect: Boolean = true

  private val eventSink: Sinks.Many<ServerMessage> = Sinks.many().unicast().onBackpressureBuffer()
  private val rawEvents = eventSink.asFlux().publish().autoConnect()
  private val connectionStatusSink: Sinks.Many<ConnectionStatus> = Sinks.many().unicast().onBackpressureBuffer()
  override val connectionStatus = connectionStatusSink.asFlux().publish().autoConnect()

  override val events = rawEvents.filter {
    it !is ServerPingMessage &&
      it !is ServerPongMessage &&
      it !is SessionResponse &&
      it !is LoginSuccessResponse &&
      it !is LoginFailedResponse
  }

  private val retrySerialFailure =
    EmitFailureHandler { _: SignalType?, emitResult: EmitResult ->
      (emitResult == EmitResult.FAIL_NON_SERIALIZED)
    }

  private val client = TcpClient.newConnection()
    .resolver(DefaultAddressResolverGroup.INSTANCE)
    .doOnResolveError { connection, throwable ->
      LOG.error("Could not find server", throwable)
      connection.dispose()
    }.doOnConnected {
      val address = it.channel().remoteAddress() as InetSocketAddress
      LOG.info("Connected to {} on port {}", address.hostName, address.port)
      it.addHandler(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
        .addHandler(LineBasedFrameDecoder(config.bufferSize))
      connection = it
    }.doOnDisconnected {
      LOG.info("Disconnected from server")
      it.dispose()
      pingDisposable?.dispose()
      connectionStatusSink.emitNext(ConnectionStatus.DISCONNECTED, retrySerialFailure)
    }

  init {
    rawEvents.filter { it is ServerPingMessage }.doOnNext { send(ClientPongMessage()) }.subscribe()
    connectionStatus.doOnNext {
      when (it) {
        ConnectionStatus.CONNECTING -> connecting = true
        ConnectionStatus.CONNECTED -> connecting = false
        ConnectionStatus.DISCONNECTED, null -> {
          connecting = false
          if (autoReconnect) {
            LOG.info("Attempting to reconnect")
            connectAndLogin(this.config).subscribeOn(Schedulers.immediate()).subscribe()
          }
        }
      }
    }.subscribe()
  }

  private fun openConnection(): Mono<out Connection> {
    outboundSink = Sinks.many().unicast().onBackpressureBuffer()
    outboundMessages = outboundSink.asFlux().publish().autoConnect()
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
              LOG.trace("Inbound message: {}", logMessage)
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
          .doOnNext {
            pingDisposable?.dispose()
            pingDisposable = pingWithDelay()
              .subscribeOn(Schedulers.single())
              .subscribe()
          }
          .then()

        val outboundMono = outbound.sendString(
          outboundMessages
            .doOnError { LOG.error("Outbound channel closed with error", it) }
            .doOnComplete { LOG.info("Outbound channel closed") }
            .doOnCancel { LOG.info("Outbound channel cancelled") }
            .doOnNext {
              if (it !is ClientPingMessage) {
                pingDisposable?.dispose()
                pingDisposable = pingWithDelay()
                  .subscribeOn(Schedulers.single())
                  .subscribe()
              }
            }
            // appending line ending is workaround due to broken encoder
            .flatMap {
              Mono.fromCallable {
                val jsonMessage = mapper.writeValueAsString(it)
                var logMessage = jsonMessage
                for (string in it.stringsToMask()) {
                  logMessage = logMessage.replace(string, LobbyProtocolMessage.CONFIDENTIAL_MASK)
                }
                LOG.trace("Outbound message: {}", logMessage)
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
      .doOnSubscribe { connectionStatusSink.emitNext(ConnectionStatus.CONNECTING, retrySerialFailure) }
  }

  override fun connectAndLogin(config: Config): Mono<LoginSuccessResponse> {
    this.config = config

    if (loginMono == null || (!connecting && (connection == null || connection?.isDisposed == true))) {
      val retry = createRetrySpec(config)
      val autoReconnect = this.autoReconnect
      this.autoReconnect = false

      authenticateOnNextSession(config)

      val loginSink = Sinks.one<LoginSuccessResponse>()
      emitNextLoginResponse(loginSink)

      loginMono = openConnection()
        .then(Mono.fromCallable { send(SessionRequest(config.version, config.userAgent)) })
        .retryWhen(retry)
        .doOnError { LOG.error("Error during connection", it) }
        .flatMap { loginSink.asMono().timeout(Duration.ofMinutes(1)) }
        .doOnNext {
          connectionStatusSink.emitNext(ConnectionStatus.CONNECTED, retrySerialFailure)
          this.autoReconnect = autoReconnect
        }.cache()
    }

    return loginMono as Mono<LoginSuccessResponse>
  }

  private fun emitNextLoginResponse(loginSink: Sinks.One<LoginSuccessResponse>) {
    LOG.debug("Starting login listener")
    rawEvents.filter {
      it is LoginSuccessResponse || it is LoginFailedResponse
    }.next().doOnNext {
      when (it) {
        is LoginSuccessResponse -> loginSink.emitValue(it, retrySerialFailure)
        is LoginFailedResponse -> loginSink.emitError(LoginException(it.text), retrySerialFailure)
      }
    }.subscribeOn(Schedulers.immediate()).subscribe()
  }

  private fun authenticateOnNextSession(config: Config) {
    LOG.debug("Starting session listener")
    rawEvents.filter {
      it is SessionResponse
    }.next().cast(SessionResponse::class.java).doOnNext { message ->
      config.tokenMono.doOnNext { token ->
        send(AuthenticateRequest(token, message.session, config.generateUid.apply(message.session)))
      }.subscribeOn(Schedulers.immediate()).subscribe()
    }.subscribe()
  }

  private fun createRetrySpec(config: Config) =
    Retry.fixedDelay(config.maxRetryAttempts, Duration.ofSeconds(config.retryWaitSeconds))
      .doBeforeRetry { retry: RetrySignal ->
        LOG.warn(
          "Could not reach server retrying: Attempt #{} of {}",
          retry.totalRetries(),
          config.maxRetryAttempts,
          retry.failure()
        )
      }.onRetryExhaustedThrow { spec, retrySignal ->
        LoginException(
          "Could not reach server after ${spec.maxAttempts} attempts",
          retrySignal.failure()
        )
      }

  override fun disconnect() {
    autoReconnect = false
    when (val conn = connection) {
      null -> LOG.warn("Attempting to disconnect while never connected")
      else -> {
        if (conn.isDisposed) {
          LOG.info("Already disconnected")
          return
        }

        LOG.info("Disconnecting from server")
        outboundSink.emitComplete(retrySerialFailure)
        conn.dispose()
      }
    }
  }

  private fun pingWithDelay(): Mono<Unit> = ping().delaySubscription(Duration.ofSeconds(minPingIntervalSeconds))

  private fun ping(): Mono<Unit> =
    Mono.fromCallable {
      send(ClientPingMessage())
    }

  private fun send(message: ClientMessage) {
    outboundSink.emitNext(message, retrySerialFailure)
  }

  private fun handle(message: ServerMessage): Mono<Unit> =
    Mono.fromCallable {
      eventSink.emitNext(message, retrySerialFailure)
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
