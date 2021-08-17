package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty
import reactor.core.publisher.Flux

/**
 * API for all social relationship related activities
 */
interface SocialApi {

  fun addFriend(playerId: Int)

  fun addFoe(playerId: Int)

  fun removeFriend(playerId: Int)

  fun removeFoe(playerId: Int)

  fun selectAvatar(url: String?)

  fun getAvailableAvatars(): Flux<Player.Avatar>
}


// ***********************
// *** SERVER MESSAGES ***
// ***********************

/**
 * Regular update on the players currently online
 */
data class PlayerInfo(
  val players: List<Player>,
) : ServerMessage

/**
 * A list of social information the server sends after login
 */
data class SocialInfo(
  val autojoin: List<String>,
  val channels: List<String>,
  val friends: List<Int>,
  val foes: List<Int>,
  val power: Int,
) : ServerMessage

/**
 * A list of avatars available to the requesting player
 */
data class AvatarListInfo(
  @JsonProperty("avatarlist")
  val avatarList: List<Player.Avatar>,
) : ServerMessage

/**
 * Party invite from another player
 */
data class PartyInvite(
  val sender: Int,
) : ServerMessage

/**
 * Notice you have been kicked from the party
 */
class PartyKick : ServerMessage

// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class AddFriendRequest(
  @JsonProperty("friend") val playerId: Int
) : ClientMessage

internal data class AddFoeRequest(
  @JsonProperty("foe") val playerId: Int
) : ClientMessage

internal data class RemoveFriendRequest(
  @JsonProperty("friend") val playerId: Int
) : ClientMessage

internal data class RemoveFoeRequest(
  @JsonProperty("foe") val playerId: Int
) : ClientMessage

interface AvatarRequest : ClientMessage {
  val action: String
}

internal class AvatarListRequest : AvatarRequest {
  override val action: String = "list_avatar"
}

internal data class SelectAvatarRequest(
  val avatar: String?
) : AvatarRequest {
  override val action: String = "select"
}
