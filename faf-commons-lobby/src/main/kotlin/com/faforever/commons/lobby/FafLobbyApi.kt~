package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import reactor.core.publisher.Flux


/**
 * Marker interface to identify messages on the Lobby protocol
 */
interface LobbyProtocolMessage {
  companion object {
    const val CONFIDENTIAL_MASK = "**********"
  }

  fun stringsToMask(): List<String> = listOf()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "command")
@JsonSubTypes(
  //FAF Client Messages
  JsonSubTypes.Type(value = HostGameRequest::class, name = "game_host"),
  JsonSubTypes.Type(value = JoinGameRequest::class, name = "game_join"),
  JsonSubTypes.Type(value = SessionRequest::class, name = "ask_session"),
  JsonSubTypes.Type(value = AddFriendRequest::class, name = "social_add"),
  JsonSubTypes.Type(value = AddFoeRequest::class, name = "social_add"),
  JsonSubTypes.Type(value = RemoveFriendRequest::class, name = "social_remove"),
  JsonSubTypes.Type(value = RemoveFoeRequest::class, name = "social_remove"),
  JsonSubTypes.Type(value = AvatarListRequest::class, name = "avatar"),
  JsonSubTypes.Type(value = SelectAvatarRequest::class, name = "avatar"),
  JsonSubTypes.Type(value = IceServerListRequest::class, name = "ice_servers"),
  JsonSubTypes.Type(value = RestoreGameSessionRequest::class, name = "restore_game_session"),
  JsonSubTypes.Type(value = ClientPingMessage::class, name = "ping"),
  JsonSubTypes.Type(value = ClientPongMessage::class, name = "pong"),
  JsonSubTypes.Type(value = ClosePlayerGameRequest::class, name = "admin"),
  JsonSubTypes.Type(value = ClosePlayerLobbyRequest::class, name = "admin"),
  JsonSubTypes.Type(value = BroadcastRequest::class, name = "admin"),
  JsonSubTypes.Type(value = InviteToPartyRequest::class, name = "invite_to_party"),
  JsonSubTypes.Type(value = AcceptInviteToPartyRequest::class, name = "accept_party_invite"),
  JsonSubTypes.Type(value = KickPlayerFromPartyRequest::class, name = "kick_player_from_party"),
  JsonSubTypes.Type(value = LeavePartyRequest::class, name = "leave_party"),
  JsonSubTypes.Type(value = UnreadyPartyRequest::class, name = "unready_party"),
  JsonSubTypes.Type(value = SelectPartyFactionsRequest::class, name = "set_party_factions"),
  JsonSubTypes.Type(value = GameMatchmakingRequest::class, name = "game_matchmaking"),
  JsonSubTypes.Type(value = MatchmakerInfoRequest::class, name = "matchmaker_info"),
  JsonSubTypes.Type(value = AuthenticateRequest::class, name = "auth"),
  JsonSubTypes.Type(value = IsReadyResponse::class, name = "is_ready_response"),
  // GPG Client Messages not directly instantiated they are only forwarded from the game so are serialized directly
)
interface ClientMessage : LobbyProtocolMessage

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "command")
@JsonSubTypes(
  //FAF Server Messages
  JsonSubTypes.Type(value = LoginSuccessResponse::class, name = "welcome"),
  JsonSubTypes.Type(value = SessionResponse::class, name = "session"),
  JsonSubTypes.Type(value = GameInfo::class, name = "game_info"),
  JsonSubTypes.Type(value = PlayerInfo::class, name = "player_info"),
  JsonSubTypes.Type(value = GameLaunchResponse::class, name = "game_launch"),
  JsonSubTypes.Type(value = MatchmakerInfo::class, name = "matchmaker_info"),
  JsonSubTypes.Type(value = MatchmakerMatchFoundResponse::class, name = "match_found"),
  JsonSubTypes.Type(value = MatchmakerMatchCancelledResponse::class, name = "match_cancelled"),
  JsonSubTypes.Type(value = SocialInfo::class, name = "social"),
  JsonSubTypes.Type(value = LoginFailedResponse::class, name = "authentication_failed"),
  JsonSubTypes.Type(value = NoticeInfo::class, name = "notice"),
  JsonSubTypes.Type(value = IceServerListResponse::class, name = "ice_servers"),
  JsonSubTypes.Type(value = AvatarListInfo::class, name = "avatar"),
  JsonSubTypes.Type(value = PartyInfo::class, name = "update_party"),
  JsonSubTypes.Type(value = PartyInvite::class, name = "party_invite"),
  JsonSubTypes.Type(value = PartyKick::class, name = "kicked_from_party"),
  JsonSubTypes.Type(value = SearchInfo::class, name = "search_info"),
  JsonSubTypes.Type(value = IrcPasswordInfo::class, name = "irc_password"),
  JsonSubTypes.Type(value = ServerPingMessage::class, name = "ping"),
  JsonSubTypes.Type(value = ServerPongMessage::class, name = "pong"),
  JsonSubTypes.Type(value = InvalidResponse::class, name = "invalid"),
  //GPG Server Messages
  JsonSubTypes.Type(value = HostGameGpgCommand::class, name = "HostGame"),
  JsonSubTypes.Type(value = JoinGameGpgCommand::class, name = "JoinGame"),
  JsonSubTypes.Type(value = ConnectToPeerGpgCommand::class, name = "ConnectToPeer"),
  JsonSubTypes.Type(value = IceMsgGpgCommand::class, name = "IceMsg"),
  JsonSubTypes.Type(value = DisconnectFromPeerGpgCommand::class, name = "DisconnectFromPeer"),
  JsonSubTypes.Type(value = IsReadyRequest::class, name = "is_ready"),
)
interface ServerMessage : LobbyProtocolMessage


/**
 * API for interacting with the FAF Lobby server
 */
interface FafLobbyApi :
  ConnectionApi,
  GameApi,
  AdminApi,
  SocialApi,
  MatchmakerApi {

  /**
   * All "public" server events can be subscribed here.
   * Internal events such as Ping or LoginResponse are filtered out.
   */
  val events: Flux<ServerMessage>

  /**
   * Emits whenever the tcp connection status changes
   */
  val connectionStatus: Flux<ConnectionStatus>
}

/// ****************
/// * SHARED ENUMS *
/// ****************

enum class Faction(
  @JsonValue
  val faString: String,
  val faIndex: Int
) {
  UEF("uef", 1),

  AEON("aeon", 2),

  CYBRAN("cybran", 3),

  SERAPHIM("seraphim", 4),

  RANDOM("random", 5),

  CIVILIAN("civilian", 6);

  companion object {
    private val fromFAString: Map<String, Faction> = values().asList().associateBy { it.faString }
    private val fromFAIndex: Map<Int, Faction> = values().asList().associateBy { it.faIndex }

    @JvmStatic
    @JsonCreator
    fun fromObject(value: Any?): Faction? {
      return when (value) {
        is Int -> fromFAIndex[value]
        is String -> fromFAString[value]
        else -> null
      }
    }
  }
}

enum class MessageTarget {
  @JsonProperty("game")
  GAME
}
