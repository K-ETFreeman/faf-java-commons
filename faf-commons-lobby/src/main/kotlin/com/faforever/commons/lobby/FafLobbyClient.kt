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
import reactor.netty.http.client.HttpClient
import reactor.netty.http.client.WebsocketClientSpec
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
    val url: String,
    val generateUid: Function<Long, String>,
    val bufferSize: Int,
    val wiretap: Boolean = false,
    val maxRetryAttempts: Long,
    val retryWaitSeconds: Long,
  )

  private lateinit var config: Config

  private var connectionDisposable: Disposable? = null
  private var connection: Connection? = null
  private var pingDisposable: Disposable? = null
  private var connecting: Boolean = false

  var minPingIntervalSeconds: Long = 60

  private var autoReconnect: Boolean = false
  private var kicked: Boolean = false
  private val outboundSink: Sinks.Many<ClientMessage> = Sinks.many().unicast().onBackpressureBuffer()
  private val outboundMessages: Flux<ClientMessage> = outboundSink.asFlux().publish().autoConnect()
  private val eventSink: Sinks.Many<ServerMessage> = Sinks.many().unicast().onBackpressureBuffer()
  private val rawEvents = eventSink.asFlux().publish().autoConnect()
  private val connectionStatusSink: Sinks.Many<ConnectionStatus> = Sinks.many().unicast().onBackpressureBuffer()
  override val connectionStatus: Flux<ConnectionStatus> = connectionStatusSink.asFlux().publish().autoConnect()
  private val connectionAcquiredSink: Sinks.Many<Any> = Sinks.many().unicast().onBackpressureBuffer()
  private val connectionAcquired: Flux<Any> = connectionAcquiredSink.asFlux().publish().autoConnect()

  override val events: Flux<ServerMessage> = rawEvents.filter {
    it !is ServerPingMessage &&
      it !is ServerPongMessage &&
      it !is SessionResponse &&
      it !is LoginSuccessResponse &&
      it !is LoginFailedResponse
  }

  private val loginResponseMono = rawEvents.ofType(LoginResponse::class.java).next().flatMap {
    when (it) {
      is LoginSuccessResponse -> Mono.just(it.me)
      is LoginFailedResponse -> Mono.error(LoginException(it.text))
    }
  }.timeout(Duration.ofSeconds(30))
    .doOnError(LoginException::class.java) { kicked = true }
    .doFirst {
      prepareAuthenticateOnNextSession()
      send(SessionRequest(config.version, config.userAgent))
    }

  private val loginMono = Mono.defer {
    connectionAcquired.next().timeout(Duration.ofSeconds(config.retryWaitSeconds))
      .then(loginResponseMono)
      .doFirst {
        openConnection()
      }.retryWhen(createRetrySpec(config))
  }
    .doOnError { LOG.error("Error during connection", it); connection?.dispose() }
    .doOnCancel { LOG.debug("Login cancelled"); disconnect() }
    .doOnSuccess {
      connectionStatusSink.emitNext(ConnectionStatus.CONNECTED, retrySerialFailure)
    }
    .doOnSubscribe { LOG.debug("Starting login process") }
    .materialize()
    .cacheInvalidateIf { it.isOnError || (!connecting && (connection == null || connection?.isDisposed == true)) }
    .dematerialize<Player>()


  private val retrySerialFailure =
    EmitFailureHandler { _: SignalType?, emitResult: EmitResult ->
      (emitResult == EmitResult.FAIL_NON_SERIALIZED)
    }

  private val httpClient = HttpClient.newConnection()
    .resolver(DefaultAddressResolverGroup.INSTANCE)
    .doOnResolveError { connection, throwable ->
      LOG.error("Could not find server", throwable)
      connection.dispose()
    }.doOnConnect {
      connection?.dispose()
    }
    .doOnConnected {
      val address = it.channel().remoteAddress() as InetSocketAddress
      LOG.info("Connected to {} on port {}", address.hostName, address.port)
      it.addHandlerFirst(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
        .addHandlerLast(LineBasedFrameDecoder(config.bufferSize))
      connection = it
      connectionAcquiredSink.emitNext(true, retrySerialFailure)
    }.doOnDisconnected {
      LOG.info("Disconnected from server")
      it.dispose()
      pingDisposable?.dispose()
      connectionStatusSink.emitNext(ConnectionStatus.DISCONNECTED, retrySerialFailure)
    }.doOnResolveError { conn, throwable ->
      LOG.error("Error resolving", throwable)
      conn.dispose()
    }

  init {
    rawEvents.filter { it is ServerPingMessage }.doOnNext { send(ClientPongMessage()) }.subscribe()
    rawEvents.ofType(NoticeInfo::class.java).filter { it.style == "kick" }.doOnNext { kicked = true }.subscribe()
    connectionStatus.doOnNext {
      when (it) {
        ConnectionStatus.CONNECTING -> connecting = true
        ConnectionStatus.CONNECTED -> connecting = false
        ConnectionStatus.DISCONNECTED, null -> {
          connecting = false
          if (autoReconnect && !kicked) {
            LOG.info("Attempting to reconnect")
            connectAndLogin(this.config).subscribeOn(Schedulers.single()).subscribe()
          }
        }
      }
    }.subscribe()
  }

  private fun openConnection() {
    LOG.debug("Opening connection")
    connectionDisposable?.dispose()
    connectionDisposable = httpClient
      .wiretap(config.wiretap)
      .websocket(WebsocketClientSpec.builder().maxFramePayloadLength(config.bufferSize).build())
      .uri(config.url)
      .handle { inbound, outbound ->
        val inboundMono = inbound.receive()
          .asString(Charsets.UTF_8)
          .flatMapIterable { it.toCharArray().asIterable() }
          .windowUntil { '\n' == it }
          .flatMap { it.collectList().map { chars -> chars.toCharArray() }.map { charArray -> String(charArray) } }
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
        ).neverComplete()

        /* The lobby protocol requires two-way communication. If either the outbound or inbound connections complete/close
           then we are better off closing the connection to the server. This is why we return a mono that completes when one
           of the connections finishes */
        Mono.firstWithSignal(inboundMono, outboundMono)
      }
      .doOnCancel { LOG.info("Connection cancelled") }
      .doOnSubscribe {
        LOG.debug("Beginning connection process")
        connectionStatusSink.emitNext(ConnectionStatus.CONNECTING, retrySerialFailure)
      }
      .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(config.retryWaitSeconds / 5))
        .doBeforeRetry { retry: RetrySignal ->
          LOG.warn(
            "Could not connect to server retrying: Attempt #{} of 5",
            retry.totalRetries(),
            retry.failure()
          )
        }.onRetryExhaustedThrow { spec, retrySignal ->
          LoginException(
            "Could not connect to server after ${spec.maxAttempts} attempts",
            retrySignal.failure()
          )
        })
      .subscribe(null, {
        LOG.warn("Error in connection", it)
        connectionStatusSink.emitNext(ConnectionStatus.DISCONNECTED, retrySerialFailure)
      }, {
        LOG.info("Connection closed")
        connectionStatusSink.emitNext(ConnectionStatus.DISCONNECTED, retrySerialFailure)
      })
  }

  override fun connectAndLogin(config: Config): Mono<Player> {
    this.config = config
    kicked = false
    autoReconnect = true
    return loginMono
  }

  private fun prepareAuthenticateOnNextSession() {
    LOG.debug("Starting session listener")
    rawEvents.ofType(SessionResponse::class.java).next()
      .zipWith(config.tokenMono).doOnNext { messageTokenTuple ->
        send(AuthenticateRequest(messageTokenTuple.t2, messageTokenTuple.t1.session, config.generateUid.apply(messageTokenTuple.t1.session)))
      }.subscribeOn(Schedulers.single()).subscribe()
  }

  private fun createRetrySpec(config: Config) =
    Retry.fixedDelay(config.maxRetryAttempts, Duration.ofSeconds(config.retryWaitSeconds))
      .filter { it !is LoginException }
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
    connection?.dispose()
  }

  private fun pingWithDelay(): Mono<Unit> = ping().delaySubscription(Duration.ofSeconds(minPingIntervalSeconds))

  private fun ping(): Mono<Unit> =
    Mono.fromCallable {
      send(ClientPingMessage())
    }

  private fun send(message: ClientMessage) {
    LOG.trace("Sending message of type {}", message.javaClass)
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
        .ofType(GameLaunchResponse::class.java)
        .next()
    )

  override fun requestJoinGame(gameId: Int, password: String?) =
    Mono.fromCallable {
      send(JoinGameRequest(gameId, password))
    }.then(
      events
        .ofType(GameLaunchResponse::class.java)
        .next()
    )

  override fun restoreGameSession(gameId: Int) = send(RestoreGameSessionRequest(gameId))

  @Deprecated("Ice Servers should be queried from the FAF API")
  override fun getIceServers(): Flux<IceServer> =
    Mono.fromCallable { send(IceServerListRequest()) }
      .thenMany(
        events
          .ofType(IceServerListResponse::class.java)
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
        .ofType(AvatarListInfo::class.java)
        .next()
        .flatMapIterable { it.avatarList }
    )

  override fun sendReady(requestId: String) = send(IsReadyResponse(requestId))

  override fun requestMatchmakerInfo() = send(MatchmakerInfoRequest())

  override fun gameMatchmaking(queueName: String, state: MatchmakerState) =
    send(GameMatchmakingRequest(queueName, state))

  override fun inviteToParty(playerId: Int) = send(InviteToPartyRequest(playerId))

  override fun acceptPartyInvite(playerId: Int) = send(AcceptInviteToPartyRequest(playerId))

  override fun kickPlayerFromParty(playerId: Int) = send(KickPlayerFromPartyRequest(playerId))

  override fun unreadyParty() = send(UnreadyPartyRequest())

  override fun leaveParty() = send(LeavePartyRequest())

  override fun setPartyFactions(factions: Set<Faction>) = send(SelectPartyFactionsRequest(factions))

  override fun sendGpgGameMessage(message: GpgGameOutboundMessage) = send(message)
}
