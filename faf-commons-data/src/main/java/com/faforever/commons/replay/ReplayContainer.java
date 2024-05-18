package com.faforever.commons.replay;

import com.faforever.commons.replay.header.ReplayHeader;

import java.util.List;

/**
 * A container of all the information that a replay may hold once parsed.
 *
 * @param metadata
 * @param header
 * @param registeredEvents
 */
public record ReplayContainer(ReplayMetadata metadata, ReplayHeader header, List<RegisteredEvent> registeredEvents) {
}
