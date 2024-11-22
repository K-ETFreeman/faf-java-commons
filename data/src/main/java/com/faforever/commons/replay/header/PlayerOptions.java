package com.faforever.commons.replay.header;

/**
 * Populated by the player options field of the table that is passed to `CLobby:LaunchGame`
 *
 * @param isHuman
 * @param aiPersonality
 * @param ratingMean
 * @param ratingDeviation
 * @param clan
 * @param isCivilian
 * @param lobbyIndex
 * @param armyName
 * @param armyColor
 * @param playerColor
 * @param playerName
 * @param ratedGamesPlayed
 * @param country
 * @param team
 * @param faction
 */
public record PlayerOptions(Boolean isHuman, String aiPersonality, Float ratingMean, Float ratingDeviation, String clan,
                            Boolean isCivilian, Integer lobbyIndex,
                            String armyName, Integer armyColor, Integer playerColor, String playerName,
                            Integer ratedGamesPlayed, String country,
                            Integer team, Integer faction) {
}
