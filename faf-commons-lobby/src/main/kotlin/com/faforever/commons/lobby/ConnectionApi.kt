package com.faforever.commons.lobby

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * API to manage connection to the Lobby server
 */
interface ConnectionApi {

  fun connectAndLogin(config: FafLobbyClient.Config): Mono<LoginSuccessResponse>

  fun disconnect()

  fun getIceServers(): Flux<IceServer>
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
 * Holds no data, just checks if the connection is still alive
 */
internal class PongMessage : ServerMessage

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
 * The server assigns us a session id, onto which we will authorize.
 * Does it make sense? Probably not.
 */
internal data class SessionResponse(
  val session: Long,
) : ServerMessage

/**
 * The server confirms a successful login and sends us our player info
 */
data class LoginSuccessResponse(
  val me: Player,
) : ServerMessage

/**
 * Randomly assigned password to login into the irc
 */
data class IrcPasswordInfo(
  val password: String,
) : ServerMessage {
  override fun stringsToMask() = listOf(password)
}

data class Player(
  val id: Int,
  val login: String,
  val clan: String?,
  val avatar: Avatar?,
  val country: String,
  val league: Map<String, String>?,
  val ratings: Map<String, LeaderboardStats>,
) {
  data class Avatar(
    val url: String,
    @JsonProperty("tooltip")
    val description: String,
  )

  data class LeaderboardStats(
    @JsonProperty("number_of_games")
    val numberOfGames: Int,
    // Two element list, element 1 is mean, element 2 is deviation
    val rating: LeaderboardRating
  ) {
    data class LeaderboardRating(
      val mean: Float,
      val deviation: Float,
    ) {
      @JsonCreator
      constructor(list: List<Float>) : this(list[0], list[1])

      @JsonValue
      fun toListFormat() : List<Float> = listOf(mean, deviation)
    }
  }
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

/**
 * List of ice servers from the lobby server
 */
data class IceServerListResponse(
  @JsonProperty("ice_servers")
  val iceServers: Collection<IceServer>,
  val ttl: Int,
) : ServerMessage


// ***********************
// *** CLIENT MESSAGES ***
// ***********************

/**
 * Authenticate with oauth token
 */
internal data class AuthenticateRequest(
  val token: String,
  val session: Long,
  @JsonProperty("unique_id")
  val uniqueId: String,
) : ClientMessage {
  override fun stringsToMask() = listOf(token, uniqueId)
}

/**
 * Request a new session
 */
internal data class SessionRequest(
  val version: String,
  @JsonProperty("user_agent")
  val userAgent: String,
) : ClientMessage

/**
 * Requests a list of ice servers, which will be returned as a [IceServerListResponse]
 */
internal class IceServerListRequest : ClientMessage

