package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API for all administrative and moderation related activities
 */
interface AdminApi {
  /**
   * Broadcasts a message that all players will receive as a [NoticeInfo]
   */
  fun broadcastMessage(message: String)

  fun closePlayerGame(playerId: Int)

  fun closePlayerLobby(playerId: Int)
}


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

interface AdminRequest : ClientMessage {
  val action: String
}

internal data class BroadcastRequest(
  val message: String,
  override val action: String = "broadcast"
) : AdminRequest

internal data class ClosePlayerGameRequest(
  @JsonProperty("user_id") val playerId: Int,
  override val action: String = "closeFA"
) : AdminRequest

internal data class ClosePlayerLobbyRequest(
  @JsonProperty("user_id") val playerId: Int,
  override val action: String = "closelobby"
) : AdminRequest
