package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeId
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
    ratingMin: Int?,
    ratingMax: Int?,
    enforceRatingRange: Boolean
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
  @JsonEnumDefaultValue
  UNKNOWN,

  @JsonProperty("custom")
  CUSTOM,

  @JsonProperty("tournament")
  TOURNAMENT,

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
  @JsonEnumDefaultValue
  UNKNOWN
}

// ***********************
// *** SERVER MESSAGES ***
// ***********************

/**
 * Info on current game or games.
 * GameInfo comes as single message or as a nested list
 * which makes all fields nullable. Thanks for nothing...
 */
data class GameInfo(
  val uid: Int?,
  val title: String?,
  val host: String?,
  @JsonProperty("game_type")
  val gameType: GameType?,
  @JsonProperty("max_players")
  val maxPlayers: Int?,
  @JsonProperty("num_players")
  val numberOfPlayers: Int?,
  val visibility: GameVisibility?,
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
  @Deprecated("Use teamIds instead")
  val teams: Map<String, List<String>>?,
  @JsonProperty("teams_ids")
  val teamIds: List<TeamIds>?,
  @JsonProperty("rating_min")
  val ratingMin: Int?,
  @JsonProperty("rating_max")
  val ratingMax: Int?,
  @JsonProperty("enforce_rating_range")
  val enforceRatingRange: Boolean?,

  val games: List<GameInfo>?,
) : ServerMessage {
  data class TeamIds (
    @JsonProperty("team_id")
    val teamId: Int,
    @JsonProperty("player_ids")
    val playerIds: List<Int>
  )
}

/**
 * Message from server containing necessary info to launch a multiplayer game
 */
data class GameLaunchResponse(
  val uid: Int,
  val name: String,
  @JsonProperty("mod")
  val featureMod: String,
  @Deprecated(message = "Use gameType instead", replaceWith = ReplaceWith("gameType"))
  @JsonProperty("init_mode")
  val lobbyMode: LobbyMode,
  @JsonProperty("game_type")
  val gameType: GameType,
  /**
   * Technical name of the leaderboard to select ratings to be shown
   */
  @JsonProperty("rating_type")
  val leaderboard: String?,
  val args: List<String>,

  @JsonProperty("mapname")
  val mapName: String? = null,
  @JsonProperty("expected_players")
  val expectedPlayers: Int? = null,
  @JsonProperty("map_position")
  val mapPosition: Int? = null,
  @JsonProperty("game_options")
  val gameOptions: Map<String, String>? = null,
  val team: Int? = null,
  val faction: Faction? = null,
) : ServerMessage


/**
 * Gpg inbound messages are message sent from other players games via ICE.
 * They are distinguishable from the [target] property.
 * You will also notice, that the command uses camel case, where the rest of the protocol uses snake case.
 */
interface GpgGameInboundMessage : ServerMessage {
  val target: MessageTarget
  val args: List<Any>

  fun getArgAsInt(index: Int): Int {
    return (args[index] as Number).toInt()
  }

  fun getArgAsBoolean(index: Int): Boolean {
    return args[index] as Boolean
  }

  fun getArgAsString(index: Int): String {
    return args[index] as String
  }

  fun <T> getArgAsObject(index: Int): T {
    return args[index] as T
  }
}

data class HostGameGpgCommand(
  override val target: MessageTarget = MessageTarget.GAME,
  override val args: List<Any>,
) : GpgGameInboundMessage {
  companion object {
    private const val MAP_INDEX = 0
  }

  fun getMap(): String {
    return getArgAsString(MAP_INDEX)
  }
}

data class JoinGameGpgCommand(
  override val target: MessageTarget = MessageTarget.GAME,
  override val args: List<Any>,
) : GpgGameInboundMessage {
  companion object {
    private const val USERNAME_INDEX = 0
    private const val PEER_UID_INDEX = 1
  }

  fun getUsername(): String {
    return getArgAsString(USERNAME_INDEX)
  }

  fun getPeerUid(): Int {
    return getArgAsInt(PEER_UID_INDEX)
  }
}

data class ConnectToPeerGpgCommand(
  override val target: MessageTarget = MessageTarget.GAME,
  override val args: List<Any>,
) : GpgGameInboundMessage {
  companion object {
    private const val USERNAME_INDEX = 0
    private const val PEER_UID_INDEX = 1
    private const val OFFER_INDEX = 2
  }

  fun getUsername(): String {
    return getArgAsString(USERNAME_INDEX)
  }

  fun getPeerUid(): Int {
    return getArgAsInt(PEER_UID_INDEX)
  }

  fun isOffer(): Boolean {
    return getArgAsBoolean(OFFER_INDEX)
  }
}

data class IceMsgGpgCommand(
  override val target: MessageTarget = MessageTarget.GAME,
  override val args: List<Any>,
) : GpgGameInboundMessage {
  companion object {
    private const val SENDER_INDEX = 0
    private const val RECORD_INDEX = 1
  }

  fun getSender(): Int {
    return getArgAsInt(SENDER_INDEX)
  }

  fun getRecord(): Any {
    return getArgAsObject(RECORD_INDEX)
  }
}

data class DisconnectFromPeerGpgCommand(
  override val target: MessageTarget = MessageTarget.GAME,
  override val args: List<Any>,
) : GpgGameInboundMessage {
  companion object {
    private const val UID_INDEX = 0
  }

  fun getUid(): Int {
    return getArgAsInt(UID_INDEX)
  }
}


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
  @JsonProperty("rating_min")
  val ratingMin: Int?,
  @JsonProperty("rating_max")
  val ratingMax: Int?,
  @JsonProperty("enforce_rating_range")
  val enforceRatingRange: Boolean,
) : ClientMessage

internal data class JoinGameRequest(
  @JsonProperty("uid")
  val gameId: Int,
  val password: String?
) : ClientMessage

internal data class RestoreGameSessionRequest(
  @JsonProperty("game_id")
  val gameId: Int,
) : ClientMessage

/**
 * Gpg outbound messages are message sent from the local running game itself and will be sent via ICE to others.
 * They are distinguishable from the [target] property.
 * You will also notice, that the command uses camel case, where the rest of the protocol uses snake case.
 */
data class GpgGameOutboundMessage(
  @JsonTypeId
  val command: String,
  val args: List<Any> = listOf(),
  val target: MessageTarget = MessageTarget.GAME,
) : ClientMessage {
  companion object {
    fun disconnectedMessage() = GpgGameOutboundMessage("Disconnected")
    fun connectedMessage() = GpgGameOutboundMessage("Connected")
    fun gameStateMessage(state: String) = GpgGameOutboundMessage("GameState", listOf(state))
    fun gameOptionMessage() = GpgGameOutboundMessage("GameOption")
    fun gameModsMessage() = GpgGameOutboundMessage("GameMods")
    fun playerOptionMessage() = GpgGameOutboundMessage("PlayerOption")
    fun disconnectFromPeerMessage() = GpgGameOutboundMessage("DisconnectFromPeer")
    fun chatMessage() = GpgGameOutboundMessage("Chat")
    fun gameResultMessage() = GpgGameOutboundMessage("GameResult")
    fun statsMessage() = GpgGameOutboundMessage("Stats")
    fun clearSlotsMessage() = GpgGameOutboundMessage("ClearSlots")
    fun aiOptionMessage() = GpgGameOutboundMessage("AIOption")
    fun jsonStatsMessage() = GpgGameOutboundMessage("JsonStats")
    fun rehostMessage() = GpgGameOutboundMessage("Rehost")
    fun desyncMessage() = GpgGameOutboundMessage("Desync")
    fun gameFullMessage() = GpgGameOutboundMessage("GameFull")
    fun iceMessage(remotePlayerId: Int, message: Any) = GpgGameOutboundMessage("IceMsg", listOf(remotePlayerId, message))
    fun connectedToHostMessage() = GpgGameOutboundMessage("ConnectedToHost")
  }
}
