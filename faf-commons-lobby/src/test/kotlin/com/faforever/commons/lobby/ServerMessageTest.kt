package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServerMessageTest {

  companion object {
    val objectMapper: ObjectMapper = ObjectMapper()
      .registerModule(KotlinModule())
      .registerModule(JavaTimeModule())
  }

  @Test
  fun deserializeLoginResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
       {"command":"welcome","me":{"id":76365,"login":"Brutus5000","avatar":{"url":"https://content.faforever.com/faf/avatars/SystemSoftware.png","tooltip":"DevOps Councillor"},"country":"DE","ratings":{"global":{"rating":[1194.13,80.9685],"number_of_games":224},"ladder_1v1":{"rating":[864.33,67.7884],"number_of_games":81},"tmm_2v2":{"rating":[1098.47,425.614],"number_of_games":1}},"global_rating":[1194.13,80.9685],"ladder_rating":[864.33,67.7884],"number_of_games":224},"id":76365,"login":"Brutus5000"}
     """.trimIndent()
    )
  }

  @Test
  fun deserializePlayerInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"player_info","players":[{"id":65341,"login":"Sheikah","country":"US","ratings":{"global":{"rating":[1625.01,79.5057],"number_of_games":1741},"ladder_1v1":{"rating":[1384.16,66.4747],"number_of_games":1725},"tmm_2v2":{"rating":[1430.5,221.442],"number_of_games":17}},"global_rating":[1625.01,79.5057],"ladder_rating":[1384.16,66.4747],"number_of_games":1741}]}
    """.trimIndent()
    )
  }

  @Test
  fun deserializeMatchmakerInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"matchmaker_info","queues":[{"queue_name":"ladder1v1","queue_pop_time":"2021-03-21T22:26:27.555251+00:00","num_players":1,"boundary_80s":[[670,1070]],"boundary_75s":[[770,970]],"team_size":1}]}
    """.trimIndent()
    )
  }

  @Test
  fun deserializeSearchInfo() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"search_info","queue_name":"ladder1v1","state":"start"}
    """.trimIndent()
    )
  }

  @Test
  fun deserializeMatchmakerMatchFoundResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"match_found","queue_name":"ladder1v1"}
    """.trimIndent()
    )
  }

  @Test
  fun deserializeHostGameGpgNetMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"HostGame","args":["8 - badlands_v4.v0001"],"target":"game"}
    """.trimIndent()
    )
  }

  @Test
  fun deserializeGameInfoMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
        {"command":"game_info","visibility":"public","password_protected":false,"uid":13757471,"title":"Unit test","state":"closed","game_type":"custom","featured_mod":"faf","sim_mods":{},"mapname":"adaptive_archsimkats_valley.v0003","map_file_path":"maps/adaptive_archsimkats_valley.v0003.zip","host":"Brutus5000","num_players":0,"max_players":12,"launched_at":null,"rating_type":"global","rating_min":null,"rating_max":null,"enforce_rating_range":false,"teams":{}}
      """.trimIndent()
    )
  }

  @Test
  fun deserializeIceMessage() {
    val result = objectMapper.readValue<ServerMessage>(
      """
        {"command":"ice_servers","ice_servers":[{"urls":["turn:test.faforever.com?transport=tcp","turn:test.faforever.com?transport=udp","stun:test.faforever.com"],"username":"1616407396:76365","credential":"banana","credentialType":"token"},{"urls":["turn:faf.mabula.net?transport=tcp","turn:faf.mabula.net?transport=udp","stun:faf.mabula.net"],"username":"1616407396:76365","credential":"banana","credentialType":"token"}],"ttl":86400}
      """.trimIndent()
    )
  }

  @Test
  fun deserializeGameLaunchResponse() {
    val result = objectMapper.readValue<ServerMessage>(
      """
      {"command":"game_launch","args":["/numgames",224],"uid":13757455,"mod":"faf","name":"Unit Test","init_mode":0,"rating_type":"global"}
    """.trimIndent()
    )

    assertEquals(
      GameLaunchResponse(
        13757455,
        "Unit Test",
        "faf",
        LobbyMode.DEFAULT_LOBBY,
        "global",
        listOf("/numgames", 224),
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

}
