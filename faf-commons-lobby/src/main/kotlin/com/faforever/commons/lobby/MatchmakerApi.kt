package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.OffsetDateTime

/**
 * API for all matchmaker related activities
 */
interface MatchmakerApi {

  /**
   * Request a [MatchmakerInfo] response.
   */
  fun requestMatchmakerInfo()

  fun gameMatchmaking(queueName: String, state: MatchmakerState)

  fun inviteToParty(playerId: Int)

  fun acceptPartyInvite(playerId: Int)

  fun kickPlayerFromParty(playerId: Int)

  fun readyParty(isReady: Boolean)

  fun leaveParty()

  fun setPartyFactions(factions: Set<Faction>)
}


enum class MatchmakerState {
  @JsonProperty("start")
  START,

  @JsonProperty("stop")
  STOP
}


// ***********************
// *** SERVER MESSAGES ***
// ***********************

data class PartyInfo(
  val owner: Int,
  val members: List<PartyMember>,
) : ServerMessage {
  data class PartyMember(
    @JsonProperty("player")
    val playerId: Int,
    val factions: List<Faction>
  )
}

data class MatchmakerInfo(
  val queues: List<MatchmakerQueue>,
) : ServerMessage {
  data class MatchmakerQueue(
    @JsonProperty("queue_name")
    val name: String,
    @JsonProperty("queue_pop_time")
    val popTime: OffsetDateTime,
    @JsonProperty("team_size")
    val teamSize: Int,
    @JsonProperty("num_players")
    val numberOfPlayers: Int,
    @JsonProperty("boundary_75s")
    val boundary75s: List<List<Int>>,
    @JsonProperty("boundary_80s")
    val boundary80s: List<List<Int>>,
  )
}

data class MatchmakerMatchFoundResponse(
  @JsonProperty("queue_name")
  val queue: String,
) : ServerMessage

class MatchmakerMatchCancelledResponse : ServerMessage

data class MatchmakingInfo(
  val state: MatchmakerState,
) : ServerMessage

data class SearchInfo(
  @JsonProperty("queue_name")
  val queueName: String,
  val state: String,
) : ServerMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class GameMatchmakingRequest(
  val queueName: String,
  val state: MatchmakerState,
) : ClientMessage {
  override val command = "game_matchmaking"
}

internal data class InviteToPartyRequest(
  @JsonProperty("recipientId")
  val playerId: Int
) : ClientMessage {
  override val command = "invite_to_party"
}

internal data class AcceptInviteToPartyRequest(
  @JsonProperty("senderId")
  val playerId: Int
) : ClientMessage {
  override val command = "accept_party_invite"
}

internal data class KickPlayerFromPartyRequest(
  @JsonProperty("kickedPlayerId")
  val playerId: Int
) : ClientMessage {
  override val command = "kick_player_from_party"
}

internal class ReadyPartyRequest(isReady: Boolean) : ClientMessage {
  override val command = if (isReady) "ready_party" else "unready_party"
}

internal class LeavePartyRequest : ClientMessage {
  override val command = "leave_party"
}

internal data class SelectPartyFactionsRequest(
  val factions: Set<Faction>
) : ClientMessage {
  override val command = "set_party_factions"
}

internal class MatchmakerInfoRequest : ClientMessage {
  override val command = "matchmaker_info"
}
