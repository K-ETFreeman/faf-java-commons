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

  fun readyParty()

  fun unreadyParty()

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

/**
 * Information on a players party sent after a change
 */
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

/**
 * Information about the matchmakers periodically sent out
 */
data class MatchmakerInfo(
  val queues: List<MatchmakerQueue>,
) : ServerMessage {
  data class MatchmakerQueue(
    @JsonProperty("queue_name")
    val name: String,
    @JsonProperty("queue_pop_time")
    val popTime: OffsetDateTime,
    @JsonProperty("queue_pop_time_delta")
    val secondsUntilPop: Float,
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

/**
 * Message when a match has been found in a queue
 */
data class MatchmakerMatchFoundResponse(
  @JsonProperty("queue_name")
  val queueName: String,
) : ServerMessage

/**
 * Message when a found match has been cancelled
 */
class MatchmakerMatchCancelledResponse : ServerMessage

/**
 * Current queue state for the player for the given queue
 */
data class SearchInfo(
  @JsonProperty("queue_name")
  val queueName: String,
  val state: MatchmakerState,
) : ServerMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class GameMatchmakingRequest(
  @JsonProperty("queue_name")
  val queueName: String,
  val state: MatchmakerState,
) : ClientMessage

internal data class InviteToPartyRequest(
  @JsonProperty("recipient_id")
  val playerId: Int
) : ClientMessage

internal data class AcceptInviteToPartyRequest(
  @JsonProperty("sender_id")
  val playerId: Int
) : ClientMessage

internal data class KickPlayerFromPartyRequest(
  @JsonProperty("kicked_player_id")
  val playerId: Int
) : ClientMessage

internal class ReadyPartyRequest : ClientMessage

internal class UnreadyPartyRequest : ClientMessage

internal class LeavePartyRequest : ClientMessage

internal data class SelectPartyFactionsRequest(
  val factions: Set<Faction>
) : ClientMessage

internal class MatchmakerInfoRequest : ClientMessage
