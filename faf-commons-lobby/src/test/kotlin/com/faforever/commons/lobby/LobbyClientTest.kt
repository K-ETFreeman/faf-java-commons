package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.LineSeparator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.netty.Connection
import reactor.netty.DisposableServer
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import reactor.test.StepVerifier
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit

class LobbyClientTest {
  companion object {
    val TIMEOUT: Long = 5000;
    val TIMEOUT_UNIT = ChronoUnit.MILLIS
    val LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress()
    val LOG: Logger = LoggerFactory.getLogger(FafLobbyClient::class.java)
  }

  private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
  private val token = "abc"
  private val serverReceivedSink = Sinks.many().unicast().onBackpressureBuffer<String>()
  private val serverMessagesReceived = serverReceivedSink.asFlux().publish().autoConnect()
  private val serverSentSink = Sinks.many().unicast().onBackpressureBuffer<String>()
  private val serverMessagesSent = serverSentSink.asFlux().publish().autoConnect()
  private lateinit var disposableServer: DisposableServer
  private lateinit var currentConnection: Connection
  private val instance: FafLobbyClient = FafLobbyClient(objectMapper)
  private val playerUid = 123
  private val sessionId: Long = 456
  private val verificationDuration = Duration.of(TIMEOUT, TIMEOUT_UNIT)
  private val retrySerialFailure =
    Sinks.EmitFailureHandler { _: SignalType?, emitResult: Sinks.EmitResult ->
      (emitResult == Sinks.EmitResult.FAIL_NON_SERIALIZED)
    }

  @BeforeEach
  fun setUp() {
    instance.minPingIntervalSeconds = Int.MAX_VALUE.toLong()
    startFakeFafLobbyServer()
    connectAndLogIn()
  }

  private fun startFakeFafLobbyServer() {
    disposableServer = TcpServer.create()
      .doOnConnection {
        LOG.debug("New Client connected to server")
        currentConnection = it
        it.addHandler(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
          .addHandler(LineBasedFrameDecoder(1024 * 1024))
      }.doOnBound { disposableServer: DisposableServer ->
        LOG.debug(
          "Fake server listening at {} on port {}",
          disposableServer.host(),
          disposableServer.port()
        )
      }.noSSL()
      .host(LOOPBACK_ADDRESS.hostAddress)
      .handle { inbound: NettyInbound, outbound: NettyOutbound ->
        val inboundMono = inbound.receive()
          .asString(StandardCharsets.UTF_8)
          .doOnNext { message: String? ->
            LOG.debug("Received message at server {}", message)
            LOG.debug("Emit Result is {}", serverReceivedSink.tryEmitNext(message!!))
          }
          .then()
        val outboundMono = outbound.sendString(
          serverMessagesSent
            .doOnNext { LOG.debug("Sending message from fake server {}", it) }
            .map { message: String ->
              message + "\n"
            }, StandardCharsets.UTF_8
        ).then()
        inboundMono.mergeWith(outboundMono)
      }
      .bindNow()
  }

  private fun commandMatches(message: String, command: String) = message.contains(
    "\"command\":\"$command\""
  )

  private fun assertCommandMatch(message: String, command: ClientMessage) =
    JSONAssert.assertEquals(objectMapper.writeValueAsString(command), message, true)

  private fun connectAndLogIn() {
    val config = FafLobbyClient.Config(
      Mono.just(token),
      "0",
      "downlords-faf-client",
      disposableServer.host(),
      disposableServer.port(),
      { "abc" },
      1024 * 1024,
      false,
      1,
      5,
      5
    )

    serverMessagesReceived.filter { commandMatches(it, "ask_session") }
      .next()
      .doOnNext {
        val sessionMessage = SessionResponse(sessionId)
        sendFromServer(sessionMessage)
      }.subscribe()

    serverMessagesReceived.filter { commandMatches(it, "auth") }
      .next()
      .doOnNext {
        val me = Player(playerUid, "Junit", null, null, "", HashMap(), HashMap())
        val loginServerMessage = LoginSuccessResponse(me)
        sendFromServer(loginServerMessage)
      }.subscribe()

    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(2))
      .assertNext { assertCommandMatch(it, SessionRequest(config.version, config.userAgent)) }
      .assertNext { assertCommandMatch(it, AuthenticateRequest(token, sessionId, config.generateUid.apply(sessionId))) }
      .expectComplete()
      .verifyLater()

    StepVerifier.create(instance.connectAndLogin(config)).expectNextCount(1).expectComplete()
      .verify(verificationDuration)

    stepVerifier.verify(verificationDuration)
  }

  private fun sendFromServer(fafServerMessage: ServerMessage) {
    serverSentSink.emitNext(objectMapper.writeValueAsString(fafServerMessage), retrySerialFailure)
  }

  @AfterEach
  fun tearDown() {
    disposableServer.disposeNow()
    instance.disconnect()
  }

  @Test
  fun testBroadcast() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, BroadcastRequest("test")) }
      .expectComplete()
      .verifyLater()

    instance.broadcastMessage("test")

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testClosePlayerGame() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClosePlayerGameRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.closePlayerGame(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testClosePlayerLobby() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClosePlayerLobbyRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.closePlayerLobby(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testHostGame() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext {
        assertCommandMatch(
          it, HostGameRequest(
            "map",
            "blah",
            "faf",
            BooleanArray(0),
            GameAccess.PUBLIC,
            0,
            null,
            GameVisibility.PUBLIC,
            null,
            null,
            false
          )
        )
      }
      .expectComplete()
      .verifyLater()

    instance.requestHostGame(
      "blah",
      "map",
      "faf",
      GameVisibility.PUBLIC,
      null,
      null,
      null,
      false,
    ).subscribe()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testJoinGame() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, JoinGameRequest(0, null)) }
      .expectComplete()
      .verifyLater()

    instance.requestJoinGame(0, null).subscribe()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testRestoreGameSession() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, RestoreGameSessionRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.restoreGameSession(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testGetIceServers() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, IceServerListRequest()) }
      .expectComplete()
      .verifyLater()

    instance.getIceServers().subscribe()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testAddFriend() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, AddFriendRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.addFriend(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testAddFoe() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, AddFoeRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.addFoe(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testRemoveFriend() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, RemoveFriendRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.removeFriend(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testRemoveFoe() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, RemoveFoeRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.removeFoe(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testSelectAvatar() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, SelectAvatarRequest(null)) }
      .expectComplete()
      .verifyLater()

    instance.selectAvatar(null)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testGetAvailableAvatars() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, AvatarListRequest()) }
      .expectComplete()
      .verifyLater()

    instance.getAvailableAvatars().subscribe()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testRequestMatchmakerInfo() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, MatchmakerInfoRequest()) }
      .expectComplete()
      .verifyLater()

    instance.requestMatchmakerInfo()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testGameMatchmaking() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, GameMatchmakingRequest("test", MatchmakerState.START)) }
      .expectComplete()
      .verifyLater()

    instance.gameMatchmaking("test", MatchmakerState.START)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testInviteToParty() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, InviteToPartyRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.inviteToParty(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testAcceptInvite() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, AcceptInviteToPartyRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.acceptPartyInvite(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testKickPlayer() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, KickPlayerFromPartyRequest(0)) }
      .expectComplete()
      .verifyLater()

    instance.kickPlayerFromParty(0)

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testReadyParty() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ReadyPartyRequest()) }
      .expectComplete()
      .verifyLater()

    instance.readyParty()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testUnreadyParty() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, UnreadyPartyRequest()) }
      .expectComplete()
      .verifyLater()

    instance.unreadyParty()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testLeaveParty() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, LeavePartyRequest()) }
      .expectComplete()
      .verifyLater()

    instance.leaveParty()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testSetPartyFactions() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, SelectPartyFactionsRequest(setOf())) }
      .expectComplete()
      .verifyLater()

    instance.setPartyFactions(setOf())

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testSendGpgGameMessage() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, GpgGameOutboundMessage("Test", listOf())) }
      .expectComplete()
      .verifyLater()

    instance.sendGpgGameMessage(GpgGameOutboundMessage("Test", listOf()))

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testPingInterval() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClientPingMessage()) }
      .expectComplete()
      .verifyLater()

    instance.minPingIntervalSeconds = 1

    sendFromServer(ServerPongMessage())

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testPingOnceInterval() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(Duration.ofSeconds(2)))
      .assertNext { assertCommandMatch(it, ClientPingMessage()) }
      .expectComplete()
      .verifyLater()

    instance.minPingIntervalSeconds = 1

    sendFromServer(ServerPongMessage())
    sendFromServer(ServerPongMessage())

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testPongInterval() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClientPingMessage()) }
      .expectComplete()
      .verifyLater()

    instance.minPingIntervalSeconds = 1

    sendFromServer(ServerPongMessage())

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testPongIntervalFailure() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClientPingMessage()) }
      .expectComplete()
      .verifyLater()

    instance.minPingIntervalSeconds = 1

    sendFromServer(ServerPongMessage())

    instance.connectionStatus.blockFirst(Duration.ofSeconds(10))

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testPingResponse() {
    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(1))
      .assertNext { assertCommandMatch(it, ClientPongMessage()) }
      .expectComplete()
      .verifyLater()

    sendFromServer(ServerPingMessage())

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testOnAuthenticationFailed() {
    instance.disconnect()

    val config = FafLobbyClient.Config(
      Mono.just(token),
      "0",
      "downlords-faf-client",
      disposableServer.host(),
      disposableServer.port(),
      { "abc" },
      1024 * 1024,
      false,
      1,
      5,
      5
    )

    val stepVerifierServer = StepVerifier.create(serverMessagesReceived.take(2))
      .assertNext { assertCommandMatch(it, SessionRequest(config.version, config.userAgent)) }
      .assertNext { assertCommandMatch(it, AuthenticateRequest(token, sessionId, config.generateUid.apply(sessionId))) }
      .expectComplete()
      .verifyLater()

    serverMessagesReceived.filter { commandMatches(it, "ask_session") }
      .next()
      .doOnNext {
        val sessionMessage = SessionResponse(sessionId)
        sendFromServer(sessionMessage)
      }.subscribe()

    serverMessagesReceived.filter { commandMatches(it, "auth") }
      .next()
      .doOnNext {
        val authenticationFailedMessage = LoginFailedResponse("boo")
        sendFromServer(authenticationFailedMessage)
      }.subscribe()

    StepVerifier.create(instance.connectAndLogin(config))
      .expectError(LoginException::class.java)
      .verify(verificationDuration)

    stepVerifierServer.verify(verificationDuration)
  }

  @Test
  fun testAutoReconnect() {
    currentConnection.dispose()

    val stepVerifier = StepVerifier.create(serverMessagesReceived.take(2))
      .assertNext { commandMatches(it, "ask_session") }
      .assertNext { commandMatches(it, "auth") }
      .expectComplete()
      .verifyLater()

    serverMessagesReceived.filter { commandMatches(it, "ask_session") }
      .next()
      .doOnNext {
        val sessionMessage = SessionResponse(sessionId)
        sendFromServer(sessionMessage)
      }.subscribe()

    serverMessagesReceived.filter { commandMatches(it, "auth") }
      .next()
      .doOnNext {
        val me = Player(playerUid, "Junit", null, null, "", HashMap(), HashMap())
        val loginServerMessage = LoginSuccessResponse(me)
        sendFromServer(loginServerMessage)
      }.subscribe()

    stepVerifier.verify(verificationDuration)
  }

  @Test
  fun testNoAutoReconnect() {
    instance.disconnect()

    StepVerifier.create(serverMessagesReceived.take(Duration.ofSeconds(5)))
      .verifyComplete()
  }
}
