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
 * @param isReady
 * @param isBadMap
 * @param lobbyIndex
 * @param armyName
 * @param armyColor
 * @param playerColor
 * @param playerName
 * @param ratedGamesPlayed
 * @param country
 * @param team
 * @param faction
 * @param sourceId
 */
public record PlayerOptions(boolean isHuman, String aiPersonality, float ratingMean, float ratingDeviation, String clan,
                            boolean isCivilian, boolean isReady, boolean isBadMap, int lobbyIndex,
                            String armyName, String armyColor, String playerColor, String playerName,
                            int ratedGamesPlayed, String country,
                            int team, int faction, int sourceId) {
}
