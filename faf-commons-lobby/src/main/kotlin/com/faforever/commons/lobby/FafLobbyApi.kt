package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import reactor.core.publisher.Flux

/**
 * Marker interface to identify messages on the Lobby protocol
 */
interface LobbyProtocolMessage

interface ClientMessage : LobbyProtocolMessage {
  val command: String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "command")
@JsonSubTypes(
  JsonSubTypes.Type(value = PingMessage::class, name = "ping"),
  JsonSubTypes.Type(value = LoginFailedResponse::class, name = "authentication_failed"),
  JsonSubTypes.Type(value = NoticeInfo::class, name = "notice"),
  JsonSubTypes.Type(value = UpdateInfo::class, name = "update"),
  JsonSubTypes.Type(value = InvalidResponse::class, name = "invalid"),
  JsonSubTypes.Type(value = SessionResponse::class, name = "session"),
  JsonSubTypes.Type(value = LoginSuccessResponse::class, name = "welcome"),
  JsonSubTypes.Type(value = PlayerInfo::class, name = "player_info"),
  JsonSubTypes.Type(value = SocialInfo::class, name = "social"),
  JsonSubTypes.Type(value = MatchmakerInfo::class, name = "matchmaker_info"),
  JsonSubTypes.Type(value = GameInfo::class, name = "game_info"),
  JsonSubTypes.Type(value = GameLaunchResponse::class, name = "game_launch"),
  JsonSubTypes.Type(value = MatchmakerMatchFoundResponse::class, name = "match_found"),
  JsonSubTypes.Type(value = MatchmakerMatchCancelledResponse::class, name = "match_cancelled"),
  JsonSubTypes.Type(value = MatchmakingInfo::class, name = "game_matchmaking"),
  JsonSubTypes.Type(value = SearchInfo::class, name = "search_info"),
  JsonSubTypes.Type(value = IceServerListResponse::class, name = "ice_servers"),
  JsonSubTypes.Type(value = PartyInfo::class, name = "update_party"),
  JsonSubTypes.Type(value = HostGameGpgCommand::class, name = "HostGame"),
  JsonSubTypes.Type(value = JoinGameGpgCommand::class, name = "JoinGame"),
  JsonSubTypes.Type(value = ConnectToPeerGpgCommand::class, name = "ConnectToPeer"),
  JsonSubTypes.Type(value = IceMsgGpgCommand::class, name = "IceMsg"),
  JsonSubTypes.Type(value = DisconnectFromPeerGpgCommand::class, name = "DisconnectFromPeer"),
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
}


/// ****************
/// * SHARED ENUMS *
/// ****************

enum class Faction {
  @JsonProperty("aeon")
  AEON,

  @JsonProperty("cybran")
  CYBRAN,

  @JsonProperty("uef")
  UEF,

  @JsonProperty("seraphim")
  SERAPHIM,

  @JsonProperty("nomad")
  NOMAD,

  @JsonProperty("civilian")
  CIVILIAN
}
