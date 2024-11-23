package com.faforever.commons.lobby

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.skyscreamer.jsonassert.JSONAssert

@Execution(ExecutionMode.CONCURRENT)
class ClientMessageTest {

  companion object {
    val objectMapper: ObjectMapper = ObjectMapper()
      .registerModule(KotlinModule.Builder().build())
      .registerModule(JavaTimeModule())
  }

  @Test
  fun serializeHostGameRequest() {
    JSONAssert.assertEquals("""
      {"command":"game_host","mapname":"name","title":"title","mod":"mod","options":[],"access":"public","version":1,"password":null,"visibility":"public","rating_min":null,"rating_max":null,"enforce_rating_range":false}
      """,
      objectMapper.writeValueAsString(
        HostGameRequest(
          "name",
          "title",
          "mod",
          BooleanArray(0),
          GameAccess.PUBLIC,
          1,
          null,
          GameVisibility.PUBLIC,
          null,
          null,
          false
        )
      ), true
    )
  }

  @Test
  fun serializeJoinGameRequest() {
    JSONAssert.assertEquals("""
    {"command":"game_join","uid":0,"password":null}
    """,
   objectMapper.writeValueAsString(JoinGameRequest(0, null)), true)
  }

  @Test
  fun serializeSessionRequest() {
    JSONAssert.assertEquals("""
    {"command":"ask_session","version":"1","user_agent":"boo"}
    """,
   objectMapper.writeValueAsString(SessionRequest("1", "boo")), true)
  }

  @Test
  fun serializeAddFriendRequest() {
    JSONAssert.assertEquals("""
    {"command":"social_add","friend":0}
    """,
   objectMapper.writeValueAsString(AddFriendRequest(0)), true)
  }

  @Test
  fun serializeAddFoeRequest() {
    JSONAssert.assertEquals("""
    {"command":"social_add","foe":0}
    """,
   objectMapper.writeValueAsString(AddFoeRequest(0)), true)
  }

  @Test
  fun serializeRemoveFriendRequest() {
    JSONAssert.assertEquals("""
    {"command":"social_remove","friend":0}
    """,
   objectMapper.writeValueAsString(RemoveFriendRequest(0)), true)
  }

  @Test
  fun serializeRemoveFoeRequest() {
    JSONAssert.assertEquals("""
    {"command":"social_remove","foe":0}
    """,
   objectMapper.writeValueAsString(RemoveFoeRequest(0)), true)
  }

  @Test
  fun serializeAvatarListRequest() {
    JSONAssert.assertEquals("""
    {"command":"avatar","action":"list_avatar"}
    """,
   objectMapper.writeValueAsString(AvatarListRequest()), true)
  }

  @Test
  fun serializeSelectAvatarRequest() {
    JSONAssert.assertEquals("""
    {"command":"avatar","avatar":"boo","action":"select"}
    """,
   objectMapper.writeValueAsString(SelectAvatarRequest("boo")), true)
  }

  @Test
  fun serializeSelectNoAvatarRequest() {
    JSONAssert.assertEquals("""
    {"command":"avatar","avatar":null,"action":"select"}
    """,
   objectMapper.writeValueAsString(SelectAvatarRequest(null)), true)
  }

  @Test
  fun serializeIceServerListRequest() {
    JSONAssert.assertEquals("""
    {"command":"ice_servers"}
    """,
   objectMapper.writeValueAsString(IceServerListRequest()), true)
  }

  @Test
  fun serializeRestoreGameSessionRequest() {
    JSONAssert.assertEquals("""
    {"command":"restore_game_session","game_id":0}
    """,
   objectMapper.writeValueAsString(RestoreGameSessionRequest(0)), true)
  }

  @Test
  fun serializePingMessage() {
    JSONAssert.assertEquals("""
      {"command":"ping"}
      """,
      objectMapper.writeValueAsString(ClientPingMessage()), true)
  }

  @Test
  fun serializePongMessage() {
    JSONAssert.assertEquals("""
    {"command":"pong"}
    """,
   objectMapper.writeValueAsString(ClientPongMessage()), true)
  }

  @Test
  fun serializeClosePlayerGameRequest() {
    JSONAssert.assertEquals("""
    {"command":"admin","user_id":0,"action":"closeFA"}
    """,
   objectMapper.writeValueAsString(ClosePlayerGameRequest(0)), true)
  }

  @Test
  fun serializeClosePlayerLobbyRequest() {
    JSONAssert.assertEquals("""
    {"command":"admin","user_id":0,"action":"closelobby"}
    """,
   objectMapper.writeValueAsString(ClosePlayerLobbyRequest(0)), true)
  }

  @Test
  fun serializeBroadcastRequest() {
    JSONAssert.assertEquals("""
    {"command":"admin","message":"boo","action":"broadcast"}
    """,
   objectMapper.writeValueAsString(BroadcastRequest("boo")), true)
  }

  @Test
  fun serializeInviteToPartyRequest() {
    JSONAssert.assertEquals("""
    {"command":"invite_to_party","recipient_id":0}
    """,
   objectMapper.writeValueAsString(InviteToPartyRequest(0)), true)
  }

  @Test
  fun serializeAcceptInviteToPartyRequest() {
    JSONAssert.assertEquals("""
    {"command":"accept_party_invite","sender_id":0}
    """,
   objectMapper.writeValueAsString(AcceptInviteToPartyRequest(0)), true)
  }

  @Test
  fun serializeKickPlayerFromPartyRequest() {
    JSONAssert.assertEquals("""
    {"command":"kick_player_from_party","kicked_player_id":0}
    """,
   objectMapper.writeValueAsString(KickPlayerFromPartyRequest(0)), true)
  }

  @Test
  fun serializeLeavePartyRequest() {
    JSONAssert.assertEquals("""
    {"command":"leave_party"}
    """,
   objectMapper.writeValueAsString(LeavePartyRequest()), true)
  }

  @Test
  fun serializeUnreadyPartyRequest() {
    JSONAssert.assertEquals("""
    {"command":"unready_party"}
    """,
   objectMapper.writeValueAsString(UnreadyPartyRequest()), true)
  }

  @Test
  fun serializeSelectPartyFactionsRequest() {
    JSONAssert.assertEquals("""
      {"command":"set_party_factions","factions":["aeon","seraphim","uef","cybran"]}
      """,
      objectMapper.writeValueAsString(
        SelectPartyFactionsRequest(
          setOf(
            Faction.AEON,
            Faction.SERAPHIM,
            Faction.UEF,
            Faction.CYBRAN
          )
        )
      ), true
    )
  }

  @Test
  fun serializeGameMatchmakingRequest() {
    JSONAssert.assertEquals("""
    {"command":"game_matchmaking","queue_name":"1","state":"start"}
    """,
   objectMapper.writeValueAsString(GameMatchmakingRequest("1", MatchmakerState.START)), true)
  }

  @Test
  fun serializeMatchmakerInfoRequest() {
    JSONAssert.assertEquals("""
    {"command":"matchmaker_info"}
    """,
   objectMapper.writeValueAsString(MatchmakerInfoRequest()), true)
  }

  @Test
  fun serializeAuthenticateRequest() {
    JSONAssert.assertEquals("""
      {"command":"auth","token":"abc","session":1,"unique_id":"def"}
      """,
      objectMapper.writeValueAsString(
        AuthenticateRequest(
          "abc",
          1,
          "def"
        )
      ), true
    )
  }
}
