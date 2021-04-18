package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * API for all social relationship related activities
 */
interface SocialApi {

  fun addFriend(playerId: Int)

  fun addFoe(playerId: Int)

  fun removeFriend(playerId: Int)

  fun removeFoe(playerId: Int)

}


// ***********************
// *** SERVER MESSAGES ***
// ***********************

/**
 * Regular update on the players currently online
 */
internal data class PlayerInfo(
  val players: List<Player>,
) : ServerMessage

/**
 * A list of social information the server sends after login
 */
internal data class SocialInfo(
  val autojoin: List<String>,
  val channels: List<String>,
  val friends: List<Int>,
  val foes: List<Int>,
  val power: Int,
) : ServerMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class AddFriendRequest(
  @JsonProperty("friend") val playerId: Int
) : ClientMessage {
  override val command = "social_add"
}

internal data class AddFoeRequest(
  @JsonProperty("foe") val playerId: Int
) : ClientMessage {
  override val command = "social_add"
}

internal data class RemoveFriendRequest(
  @JsonProperty("friend") val playerId: Int
) : ClientMessage {
  override val command = "social_remove"
}

internal data class RemoveFoeRequest(
  @JsonProperty("foe") val playerId: Int
) : ClientMessage {
  override val command = "social_remove"
}
