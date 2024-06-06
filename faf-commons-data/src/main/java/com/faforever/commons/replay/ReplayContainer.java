package com.faforever.commons.replay;

import com.faforever.commons.replay.header.ReplayHeader;

import java.util.List;

/**
 * A container of all the information that a replay may hold once parsed.
 *
 * @param metadata          Metadata that is attached to the FAF replay format.
 * @param header            The header of the replay that represents the scenario.
 * @param registeredEvents  The events of the replay that represent the input of players.
 */
public record ReplayContainer(ReplayMetadata metadata, ReplayHeader header, List<RegisteredEvent> registeredEvents) {
}
