package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.LineEncoder
import io.netty.handler.codec.string.LineSeparator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import reactor.netty.Connection
import reactor.netty.DisposableServer
import reactor.netty.NettyInbound
import reactor.netty.NettyOutbound
import reactor.netty.tcp.TcpServer
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.temporal.ChronoUnit

class LobbyClientTest {
  companion object {
    val TIMEOUT = 5000;
    val TIMEOUT_UNIT = ChronoUnit.MILLIS
    val LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress()
    val LOG: Logger = LoggerFactory.getLogger(FafLobbyClient::class.java)
  }

  private val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())
    .registerModule(JavaTimeModule())
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE);
  private val token = "abc"
  private val serverReceivedSink = Sinks.many().replay().latest<String>()
  private val serverMessagesReceived = serverReceivedSink.asFlux()
  private val serverSentSink = Sinks.many().unicast().onBackpressureBuffer<String>()
  private lateinit var disposableServer: DisposableServer
  private val instance: FafLobbyClient = FafLobbyClient(objectMapper)

  @BeforeEach
  fun setUp() {
    startFakeFafLobbyServer()
    connectAndLogIn()
  }

  private fun startFakeFafLobbyServer() {
    disposableServer = TcpServer.create()
      .doOnConnection { connection: Connection ->
        LOG.debug("New Client connected to server")
        connection.addHandler(LineEncoder(LineSeparator.UNIX)) // TODO: This is not working. Raise a bug ticket! Workaround below
          .addHandler(LineBasedFrameDecoder(1024 * 1024))
      }
      .doOnBound { disposableServer: DisposableServer ->
        LOG.debug(
          "Fake server listening at {} on port {}",
          disposableServer.host(),
          disposableServer.port()
        )
      }
      .noSSL()
      .host(LOOPBACK_ADDRESS.hostAddress)
      .handle { inbound: NettyInbound, outbound: NettyOutbound ->
        val inboundMono = inbound.receive()
          .asString(StandardCharsets.UTF_8)
          .doOnNext { message: String? ->
            LOG.debug("Received message at server {}", message)
            LOG.debug(
              "Emit Result is {}",
              serverReceivedSink.tryEmitNext(message!!)
            )
          }
          .then()
        val outboundMono = outbound.sendString(
          serverSentSink.asFlux()
            .map { message: String ->
              LOG.debug(
                "Sending message from fake server {}",
                message
              )
              """
              $message

              """.trimIndent()
            }, StandardCharsets.UTF_8
        ).then()
        inboundMono.mergeWith(outboundMono)
      }
      .bindNow()
  }

  private fun assertMessageCommandTypeSent(command: String) {
    assertTrue(serverMessagesReceived.any {
      it.contains(
        "\"command\":\"$command\""
      )
    }.switchIfEmpty(Mono.just(false)).block(Duration.of(TIMEOUT.toLong(), TIMEOUT_UNIT)) == true)
  }

  private fun connectAndLogIn() {
    val playerUid = 123
    val sessionId: Long = 456
    val config = FafLobbyClient.Config(
      token,
      "0",
      "downlords-faf-client",
      disposableServer.host(),
      disposableServer.port(),
      { "abc" },
      1024 * 1024,
      false
    )
    instance.connectAndLogin(config).subscribe()
    assertMessageCommandTypeSent("ask_session")
    val sessionMessage = SessionResponse(sessionId)
    sendFromServer(sessionMessage)
    assertMessageCommandTypeSent("auth")
    val me = Player(playerUid, "Junit", null, null, "", HashMap(), HashMap())
    val loginServerMessage = LoginSuccessResponse(me)
    sendFromServer(loginServerMessage)
  }

  private fun sendFromServer(fafServerMessage: ServerMessage) {
    serverSentSink.tryEmitNext(objectMapper.writeValueAsString(fafServerMessage))
  }

  @AfterEach
  fun tearDown() {
    disposableServer.disposeNow()
    instance.disconnect()
  }

  @Test
  fun testBroadcast() {
    instance.broadcastMessage("test")

    assertMessageCommandTypeSent("admin")
  }

  @Test
  fun testClosePlayerGame() {
    instance.closePlayerGame(0)

    assertMessageCommandTypeSent("admin")
  }

  @Test
  fun testClosePlayerLobby() {
    instance.closePlayerLobby(0)

    assertMessageCommandTypeSent("admin")
  }

  @Test
  fun testHostGame() {
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

    assertMessageCommandTypeSent("game_host")
  }

  @Test
  fun testJoinGame() {
    instance.requestJoinGame(0, null).subscribe()

    assertMessageCommandTypeSent("game_join")
  }

  @Test
  fun testRestoreGameSession() {
    instance.restoreGameSession(0)

    assertMessageCommandTypeSent("restore_game_session")
  }

  @Test
  fun testGetIceServers() {
    instance.getIceServers().subscribe()

    assertMessageCommandTypeSent("ice_servers")
  }

  @Test
  fun testAddFriend() {
    instance.addFriend(0)

    assertMessageCommandTypeSent("social_add")
  }

  @Test
  fun testAddFoe() {
    instance.addFoe(0)

    assertMessageCommandTypeSent("social_add")
  }

  @Test
  fun testRemoveFriend() {
    instance.removeFriend(0)

    assertMessageCommandTypeSent("social_remove")
  }

  @Test
  fun testRemoveFoe() {
    instance.removeFoe(0)

    assertMessageCommandTypeSent("social_remove")
  }

  @Test
  fun testSelectAvatar() {
    instance.selectAvatar(null)

    assertMessageCommandTypeSent("avatar")
  }

  @Test
  fun testGetAvailableAvatars() {
    instance.getAvailableAvatars().subscribe()

    assertMessageCommandTypeSent("avatar")
  }

  @Test
  fun testRequestMatchmakerInfo() {
    instance.requestMatchmakerInfo()

    assertMessageCommandTypeSent("matchmaker_info")
  }

  @Test
  fun testGameMatchmaking() {
    instance.gameMatchmaking("test", MatchmakerState.START)

    assertMessageCommandTypeSent("game_matchmaking")
  }

  @Test
  fun testInviteToParty() {
    instance.inviteToParty(0)

    assertMessageCommandTypeSent("invite_to_party")
  }

  @Test
  fun testAcceptInvite() {
    instance.acceptPartyInvite(0)

    assertMessageCommandTypeSent("accept_party_invite")
  }

  @Test
  fun testKickPlayer() {
    instance.kickPlayerFromParty(0)

    assertMessageCommandTypeSent("kick_player_from_party")
  }

  @Test
  fun testReadyParty() {
    instance.readyParty()

    assertMessageCommandTypeSent("ready_party")
  }

  @Test
  fun testUnreadyParty() {
    instance.unreadyParty()

    assertMessageCommandTypeSent("unready_party")
  }

  @Test
  fun testLeaveParty() {
    instance.leaveParty()

    assertMessageCommandTypeSent("leave_party")
  }

  @Test
  fun testSetPartyFactions() {
    instance.setPartyFactions(setOf())

    assertMessageCommandTypeSent("set_party_factions")
  }

  @Test
  fun testSendGpgGameMessage() {
    instance.sendGpgGameMessage(GpgGameOutboundMessage("Test", listOf()))

    assertMessageCommandTypeSent("Test")
  }
}
