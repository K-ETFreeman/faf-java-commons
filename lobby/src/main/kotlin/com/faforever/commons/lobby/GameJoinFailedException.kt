package com.faforever.commons.lobby

class GameJoinFailedException(
  val gameId: Int,
  val failureReason: String?
) : RuntimeException()
