package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import reactor.core.publisher.Mono

/**
 * API for all game related activities
 */
interface GameApi {

  fun requestHostGame(
    title: String,
    mapName: String,
    mod: String,
    visibility: GameVisibility,
    password: String?,
  ): Mono<GameLaunchResponse>

  fun requestJoinGame(gameId: Int, password: String?): Mono<GameLaunchResponse>

  fun restoreGameSession(gameId: Int)

  fun sendGpgGameMessage(message: GpgGameOutboundMessage)
}


// ********************
// *** SHARED ENUMS ***
// ********************


enum class GameStatus {
  @JsonProperty("unknown")
  UNKNOWN,

  @JsonProperty("playing")
  PLAYING,

  @JsonProperty("open")
  OPEN,

  @JsonProperty("closed")
  CLOSED;
}

@JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
enum class LobbyMode {
  @JsonProperty(index = 0)
  DEFAULT_LOBBY,

  @JsonProperty(index = 1)
  AUTO_LOBBY
}

enum class GameVisibility {
  @JsonProperty("public")
  PUBLIC,

  @JsonProperty("friends")
  PRIVATE
}

enum class GameAccess {
  @JsonProperty("public")
  PUBLIC,

  @JsonProperty("password")
  PASSWORD
}

enum class GameType {
  @JsonProperty("unknown")
  UNKNOWN,

  @JsonProperty("custom")
  CUSTOM,

  @JsonProperty("matchmaker")
  MATCHMAKER,

  @JsonProperty("coop")
  COOP,

  @JsonProperty("tutorial")
  TUTORIAL,
}

enum class VictoryCondition {
  @JsonProperty(index = 0)
  DEMORALIZATION,

  @JsonProperty(index = 1)
  DOMINATION,

  @JsonProperty(index = 2)
  ERADICATION,

  @JsonProperty(index = 3)
  SANDBOX,

  @JsonProperty("unknown")
  UNKNOWN
}

// ***********************
// *** SERVER MESSAGES ***
// ***********************

/**
 * GameInfo comes as single message or as a nested list
 * which makes all fields nullable. Thanks for nothing...
 */
data class GameInfo(
  val uid: Long?,
  val title: String?,
  val host: String?,
  @JsonProperty("game_type")
  val gameType: GameType?,
  @JsonProperty("max_players")
  val maxPlayers: Int?,
  @JsonProperty("num_players")
  val numberOfPlayers: Int?,
  val visibility: String?,
  @JsonProperty("password_protected")
  val passwordProtected: Boolean?,
  val state: GameStatus?,
  @JsonProperty("featured_mod")
  val featuredMod: String?,
  @JsonProperty("rating_type")
  val leaderboard: String?,
  @JsonProperty("sim_mods")
  val simMods: Map<String, String>?,
  @JsonProperty("mapname")
  val mapName: String?,
  @JsonProperty("map_file_path")
  val mapFilePath: String?,
  @JsonProperty("launched_at")
  val launchedAt: Double?,
  val teams: Map<String, List<String>>?,
  @JsonProperty("rating_min")
  val ratingMin: Int?,
  @JsonProperty("rating_max")
  val ratingMax: Int?,
  @JsonProperty("enforce_rating_range")
  val enforceRatingRange: Boolean?,

  val games: List<GameInfo>?,
) : ServerMessage

data class GameLaunchResponse(
  val uid: Int,
  val name: String,
  @JsonProperty("mod")
  val featureMod: String,
  @JsonProperty("init_mode")
  val lobbyMode: LobbyMode,
  /**
   * Technical name of the leaderboard to select ratings to be shown
   */
  @JsonProperty("rating_type")
  val leaderboard: String,
  val args: List<Any>,

  @JsonProperty("mapname")
  val mapName: String? = null,
  @JsonProperty("expected_players")
  val expectedPlayers: Int? = null,
  @JsonProperty("map_position")
  val mapPosition: Int? = null,
  val team: Int? = null,
  val faction: Faction? = null,
) : ServerMessage


/**
 * Gpg inbound messages are message sent from other players games via ICE.
 * They are distinguishable from the [target] property.
 * You will also notice, that the command uses camel case, where the rest of the protocol uses snake case.
 */
interface GpgGameInboundMessage : ServerMessage {
  val target: String
  val args: List<Any>
}

data class HostGameGpgCommand(
  override val target: String,
  override val args: List<Any>,
) : GpgGameInboundMessage

data class JoinGameGpgCommand(
  override val target: String,
  override val args: List<Any>,
) : GpgGameInboundMessage

data class ConnectToPeerGpgCommand(
  override val target: String,
  override val args: List<Any>,
) : GpgGameInboundMessage

data class IceMsgGpgCommand(
  override val target: String,
  override val args: List<Any>,
) : GpgGameInboundMessage

data class DisconnectFromPeerGpgCommand(
  override val target: String,
  override val args: List<Any>,
) : GpgGameInboundMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class HostGameRequest(
  @JsonProperty("mapname")
  val mapName: String,
  val title: String,
  val mod: String,
  val options: BooleanArray,
  val access: GameAccess,
  val version: Int,
  val password: String?,
  val visibility: GameVisibility,
) : ClientMessage {
  override val command = "game_host"
}

internal data class JoinGameRequest(
  @JsonProperty("uid")
  val gameId: Int,
  val password: String?
) : ClientMessage {
  override val command = "game_join"
}

internal data class RestoreGameSessionRequest(
  @JsonProperty("game_id")
  val gameId: Int,
) : ClientMessage {
  override val command = "restore_game_session"
}

/**
 * Gpg outbound messages are message sent from the local running game itself and will be sent via ICE to others.
 * They are distinguishable from the [target] property.
 * You will also notice, that the command uses camel case, where the rest of the protocol uses snake case.
 */
data class GpgGameOutboundMessage(
  override val command: String,
  val args: List<Any>,
) : ClientMessage {
  val target = "game"
}
