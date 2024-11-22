package com.faforever.commons.replay.header;

/**
 * Populated by the engine, it represents the client identifiers and names of players that are connected to the game. The list includes observers.
 * @param name
 * @param playerId
 * @param sourceId
 */
public record Source(int sourceId, int playerId, String name) {
}
