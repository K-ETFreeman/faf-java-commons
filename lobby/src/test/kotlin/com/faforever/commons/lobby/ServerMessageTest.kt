package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.OffsetDateTime

@Execution(ExecutionMode.CONCURRENT)
class ServerMessageTest {

  companion object {
    val objectMapper: ObjectMapper = ObjectMapper()
      .registerModule(KotlinModule())
      .registerModule(JavaTimeModule())
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  }

  @Test
  fun deserializeLoginResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
       {"command":"welcome","me":{"id":76365,"login":"Brutus5000","avatar":{"url":"https://content.faforever.com/faf/avatars/SystemSoftware.png","tooltip":"DevOps Councillor"},"country":"DE","ratings":{"global":{"rating":[1194.13,80.9685],"number_of_games":224}},"state":"offline"}}
     """.trimIndent()
    )
    assertEquals(
      LoginSuccessResponse(
        Player(
          76365,
          "Brutus5000",
          null,
          Player.Avatar("https://content.faforever.com/faf/avatars/SystemSoftware.png", "DevOps Councillor"),
          "DE",
          null,
          mapOf(
            "global" to Player.LeaderboardStats(224, Player.LeaderboardStats.LeaderboardRating(1194.13f, 80.9685f))),
          Player.State.OFFLINE
        )
      ), result
    )
  }

  @Test
  fun deserializePlayerInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"player_info","players":[{"id":65341,"login":"Sheikah","country":"US","ratings":{"ladder_1v1":{"rating":[1384.16,66.4747],"number_of_games":1725}}}]}
    """.trimIndent()
    )
    assertEquals(
      PlayerInfo(
        listOf(
          Player(
            65341, "Sheikah", null, null, "US", null, mapOf(
            "ladder_1v1" to Player.LeaderboardStats(
              1725,
              Player.LeaderboardStats.LeaderboardRating(1384.16f, 66.4747f)
            )), null
          )
        )
      ), result
    )
  }

  @Test
  fun deserializeMatchmakerInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"matchmaker_info","queues":[{"queue_name":"ladder1v1","queue_pop_time":"2021-03-21T22:26:27.555251+00:00","queue_pop_time_delta":10, "num_players":1,"boundary_80s":[[670,1070]],"boundary_75s":[[770,970]],"team_size":1}]}
    """.trimIndent()
    )
    assertEquals(
      MatchmakerInfo(
        listOf(
          MatchmakerInfo.MatchmakerQueue(
            "ladder1v1", OffsetDateTime.parse("2021-03-21T22:26:27.555251+00:00"),
            10f,
            1,
            1,
            listOf(listOf(770, 970)),
            listOf(listOf(670, 1070))
          )
        )
      ), result
    )
  }

  @Test
  fun deserializeNoticeInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"notice","style":"kill","text":"boo"}
    """.trimIndent()
    )
    assertEquals(NoticeInfo("kill", "boo"), result)
  }

  @Test
  fun deserializeSessionResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"session","session":1234}
    """.trimIndent()
    )
    assertEquals(SessionResponse(1234), result)
  }

  @Test
  fun deserializeSearchInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"search_info","queue_name":"ladder1v1","state":"start"}
    """.trimIndent()
    )
    assertEquals(SearchInfo("ladder1v1", MatchmakerState.START), result)
  }

  @Test
  fun deserializeMatchmakerMatchFoundResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"match_found","queue_name":"ladder1v1"}
    """.trimIndent()
    )
    assertEquals(MatchmakerMatchFoundResponse("ladder1v1"), result)
  }

  @Test
  fun deserializeLoginFailedResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"authentication_failed","text":"boo"}
    """.trimIndent()
    )
    assertEquals(LoginFailedResponse("boo"), result)
  }

  @Test
  fun deserializeMatchmakerMatchCancelledResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"match_cancelled"}
    """.trimIndent()
    )
    assertEquals(MatchmakerMatchCancelledResponse::class, result::class)
  }

  @Test
  fun deserializeAvatarListInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"avatar", "avatarlist":[{"url" : "google.com", "tooltip": "boo"}]}
    """.trimIndent()
    )
    assertEquals(AvatarListInfo(listOf(Player.Avatar("google.com", "boo"))), result)
  }

  @Test
  fun deserializePartyInvite() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"party_invite", "sender":1}
    """.trimIndent()
    )
    assertEquals(PartyInvite(1), result)
  }

  @Test
  fun deserializePartyKick() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"kicked_from_party"}
    """.trimIndent()
    )
    assertEquals(PartyKick::class, result::class)
  }

  @Test
  fun deserializeHostGameGpgNetMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"HostGame","args":["8 - badlands_v4.v0001"],"target":"game"}
    """.trimIndent()
    )

    assertEquals(HostGameGpgCommand(args = listOf("8 - badlands_v4.v0001")), result)
  }

  @Test
  fun deserializeGameInfoMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
        {"command":"game_info","visibility":"public","password_protected":false,"uid":13757471,"title":"Unit test","state":"closed","game_type":"custom","featured_mod":"faf","sim_mods":{},"mapname":"adaptive_archsimkats_valley.v0003","map_file_path":"maps/adaptive_archsimkats_valley.v0003.zip","host":"Brutus5000","num_players":0,"max_players":12,"hosted_at":"2024-02-06T12:45:50.000000010Z","launched_at":null,"rating_type":"global","rating_min":null,"rating_max":null,"enforce_rating_range":false,"teams":{}}
      """.trimIndent()
    )

    assertEquals(
      GameInfo(
        13757471,
        "Unit test",
        "Brutus5000",
        GameType.CUSTOM,
        12,
        0,
        GameVisibility.PUBLIC,
        false,
        GameStatus.CLOSED,
        "faf",
        "global",
        mapOf(),
        "adaptive_archsimkats_valley.v0003",
        "maps/adaptive_archsimkats_valley.v0003.zip",
        "2024-02-06T12:45:50.000000010Z",
        null,
        mapOf(),
        null,
        null,
        null,
        false,
        null
      ), result
    )
  }

  @Test
  fun deserializeIceMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
        {"command":"ice_servers","ice_servers":[{"urls":["turn:test.faforever.com?transport=tcp","turn:test.faforever.com?transport=udp","stun:test.faforever.com"],"username":"1616407396:76365","credential":"banana","credentialType":"token"},{"urls":["turn:faf.mabula.net?transport=tcp","turn:faf.mabula.net?transport=udp","stun:faf.mabula.net"],"username":"1616407396:76365","credential":"banana","credentialType":"token"}],"ttl":86400}
      """.trimIndent()
    )

    assertEquals(
      IceServerListResponse(
        listOf(
          IceServer(
            null,
            listOf(
              "turn:test.faforever.com?transport=tcp",
              "turn:test.faforever.com?transport=udp",
              "stun:test.faforever.com"
            ),
            "1616407396:76365",
            "banana",
            "token"
          ),
          IceServer(
            null,
            listOf(
              "turn:faf.mabula.net?transport=tcp", "turn:faf.mabula.net?transport=udp", "stun:faf.mabula.net"
            ),
            "1616407396:76365",
            "banana",
            "token"
          )
        ), 86400
      ),
      result
    )
  }

  @Test
  fun deserializeGameLaunchResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"game_launch","args":["/numgames",224],"uid":13757455,"mod":"faf","name":"Unit Test","init_mode":0,"rating_type":"global","game_type":"custom"}
    """.trimIndent()
    )

    assertEquals(
      GameLaunchResponse(
        13757455,
        "Unit Test",
        "faf",
        LobbyMode.DEFAULT_LOBBY,
        GameType.CUSTOM,
        "global",
        listOf("/numgames", "224"),
      ),
      result
    )
  }

  @Test
  fun deserializeMatchmakerGameLaunchResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"game_launch","args":["/numgames",224],"uid":13757455,"mod":"faf","name":"Unit Test","init_mode":0,"game_options":{"UnitCap":500,"Observers":true, "Share":"FullShare"},"rating_type":"global","game_type":"custom","mapname":"test","expected_players":2,"map_position":1,"team":1,"faction":3}
    """.trimIndent()
    )

    assertEquals(
      GameLaunchResponse(
        13757455,
        "Unit Test",
        "faf",
        LobbyMode.DEFAULT_LOBBY,
        GameType.CUSTOM,
        "global",
        listOf("/numgames", "224"),
        "test",
        2,
        1,
        mapOf("UnitCap" to "500", "Observers" to "true", "Share" to "FullShare"),
        1,
        Faction.CYBRAN,
      ),
      result
    )
  }

  @Test
  fun deserializePartyInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"update_party","owner":76365,"members":[{"player":76365,"factions":["uef"]}]}
    """.trimIndent()
    )

    assertEquals(
      PartyInfo(
        76365,
        listOf(PartyInfo.PartyMember(76365, listOf(Faction.UEF)))
      ),
      result
    )
  }

  @Test
  fun deserializePingMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"ping"}
    """.trimIndent()
    )

    assertTrue(result is ServerPingMessage)
  }

  @Test
  fun deserializePongMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"pong"}
    """.trimIndent()
    )

    assertTrue(result is ServerPongMessage)
  }
}
