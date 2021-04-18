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

internal data class BroadcastRequest(
  val message: String,
) : ClientMessage {
  override val command = "broadcast"
}

internal data class ClosePlayerGameRequest(
  @JsonProperty("user_id") val playerId: Int,
) : ClientMessage {
  override val command = "closeFA"
}

internal data class ClosePlayerLobbyRequest(
  @JsonProperty("user_id") val playerId: Int,
) : ClientMessage {
  override val command = "closelobby"
}
