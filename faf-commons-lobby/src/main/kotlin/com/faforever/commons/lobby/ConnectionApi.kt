package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonProperty
import reactor.core.publisher.Mono

/**
 * API to manage connection to the Lobby server
 */
interface ConnectionApi {

  fun connectAndLogin(): Mono<LoginSuccessResponse>

  fun disconnect()

  fun getIceServers(): Mono<Collection<IceServer>>
}

class LoginException(reason: String?) : Exception(reason)


// ***********************
// *** SERVER MESSAGES ***
// ***********************

/**
 * Holds no data, just checks if the connection is still alive
 */
internal class PingMessage : ServerMessage

/**
 * Indicates the previous client message could not be parsed.
 * The server closes the connection after sending this response.
 */
internal class InvalidResponse : ServerMessage

/**
 * A general message from the server (automated or broadcast from admin) to display to the user.
 * FIXME: Should maybe offer event codes for translations inc ase of automated responses.
 */
data class NoticeInfo(
  val style: String?,
  val text: String?,
) : ServerMessage

/**
 * The server could inform you that there is a newer version.
 * This was used for older Python implementations. You will probably never encounter it.
 */
data class UpdateInfo(
  val update: String?,
  @JsonProperty("new_version")
  val newVersion: String?,
) : ServerMessage

/**
 * The server assigns us a session id, onto which we will authorize.
 * Does it make sense? Probably not.
 */
internal data class SessionResponse(
  val session: Long,
) : ServerMessage

data class LoginSuccessResponse(
  val id: Int,
  val login: String,
  val me: Player,
) : ServerMessage

data class Player(
  val id: Int,
  val login: String,
  val clan: String?,
  val avatar: Avatar?,
  @JsonProperty("global_rating")
  val globalRating: List<Float>?,
  @JsonProperty("ladder_rating")
  val ladderRating: List<Float>?,
  @JsonProperty("number_of_games")
  val numberOfGames: Int,
  val country: String,
  val league: Map<String, String>?,
  val ratings: Map<String, LeaderboardRating>,
) {
  data class Avatar(
    val url: String,
    @JsonProperty("tooltip")
    val description: String,
  )

  data class LeaderboardRating(
    @JsonProperty("number_of_games")
    val numberOfGame: Int,
    val rating: List<Float>
  )
}

/**
 * Response if the login failed with an all english response on what failed.
 * FIXME: This should send error codes instead of translated text.
 */
internal data class LoginFailedResponse(
  val text: String?,
) : ServerMessage

data class IceServer(
  val url: String?,
  val urls: Collection<String>?,
  val username: String,
  val credential: String,
  val credentialType: String,
)

data class IceServerListResponse(
  @JsonProperty("ice_servers")
  val iceServers: Collection<IceServer>,
  val ttl: Int,
) : ServerMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

internal data class LoginRequest(
  val login: String,
  val password: String,
  val session: Long,
  @JsonProperty("unique_id")
  val uniqueId: String?,
  @JsonProperty("local_ip")
  val localIp: String,
) : ClientMessage {
  override val command = "hello"
}

internal data class SessionRequest(
  val version: String = "1.0",
  @JsonProperty("user_agent")
  val userAgent: String = "downlords-faf-client",
) : ClientMessage {
  override val command = "ask_session"
}

/**
 * Requests a list of ice servers, which will be returned as a [IceServerListResponse]
 */
internal class IceServerListRequest : ClientMessage {
  override val command = "ice_servers"
}

