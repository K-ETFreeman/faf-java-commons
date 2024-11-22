package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.faforever.commons.replay.header.Source;

/**
 * Combines the tick and source of an event. The tick represents when the event was registered. The source represents who authorised the event.
 *
 * @param tick
 * @param source
 * @param event
 * @see Event
 */
public record RegisteredEvent(int tick, Source source, Event event) {
}
