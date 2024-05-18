package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.faforever.commons.replay.header.Source;
import com.faforever.commons.replay.shared.LuaData;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ReplaySemantics {

  public static Duration tickToDuration(int tick) {
    return Duration.ofSeconds(tick / 10);
  }

  /**
   * Registers the events by attaching a tick and a source to them.
   * @param sources All input sources of the replay
   * @param events  All events of the replay
   * @return All events with the tick and input source attached
   */
  public static List<RegisteredEvent> registerEvents(List<Source> sources, List<Event> events) {
    final AtomicInteger tick = new AtomicInteger(0);
    final AtomicInteger commandSourceId = new AtomicInteger(-1);

    return events.stream().map((event) -> switch (event) {
      case Event.Advance(int ticksToAdvance) -> {
        tick.addAndGet(ticksToAdvance);
        yield null;
      }

      case Event.SetCommandSource (int playerIndex) -> {
        commandSourceId.set(playerIndex);
        yield null;
      }

      default -> new RegisteredEvent(tick.intValue(), sources.get(commandSourceId.intValue()), event);
    }).filter(Objects::nonNull).toList();
  }

  /**
   * Retrieves all events that are chat messages
   *
   * @param events A list of events
   * @return A list of events that are chat messages
   */
  public static List<ChatMessage> findChatMessages(List<Source> sources, List<RegisteredEvent> events) {
    return events.stream().map((registeredEvent) -> switch (registeredEvent.event()) {

      // TODO: the fact that we piggy-back on the 'GiveResourcesToPlayer' callback to embed chat messages is all wrong! We should instead introduce an alternative callback with the sole purpose to send messages.
      //  Requires refactoring in the game!

      case Event.LuaSimCallback(
        String func, LuaData.Table callbackTable, Event.CommandUnits commandUnits
      ) when func.equals("GiveResourcesToPlayer") -> {
        // TODO: this field has no meaning and can be manipulated, instead use the authorised command source.
        //  Requires refactoring in the game!
        if (!(callbackTable.value().get("From") instanceof LuaData.Number (float from))) {
          yield null;
        }

        // focus army starts is 1-based instead of 0-based, to align it we subtract 1
        if (from - 1 <= -2) {
          yield null;
        }

        // TODO: this field has no meaning and can be manipulated, instead use the authorised command source.
        //  Requires refactoring in the game!
        if (!(callbackTable.value().get("Sender") instanceof LuaData.String (String sender))) {
          yield null;
        }

        // TODO: apparently all players create a sim callback that contains the chat message. This hack is how we skip it,
        //   Requires refactoring in the game!
        if (!Objects.equals(sender, registeredEvent.source().name())) {
          yield null;
        }

        if (!(callbackTable.value().get("Msg") instanceof LuaData.Table (Map<java.lang.String, LuaData> msgTable))) {
          yield null;
        }

        // TODO: this is 1 out of the 2 legitimate fields
        if (!(msgTable.get("to") instanceof LuaData.String msgTo)) {
          yield null;
        }

        // TODO: this is 2 out of the 2 legitimate fields
        if (!(msgTable.get("text") instanceof LuaData.String msgText)) {
          yield null;
        }

        yield new ChatMessage(tickToDuration(registeredEvent.tick()), registeredEvent.source().name(), msgTo.value(), msgText.value());
      }
      default -> null;
    }).filter(Objects::nonNull).toList();
  }

  /**
   * Retrieves all events that are moderator related
   *
   * @param events A list of events
   * @return A list of events that are moderator related
   */
  public static List<ModeratorEvent> findModeratorMessages(List<Source> sources, List<RegisteredEvent> events) {
    return events.stream().map((registeredEvent) -> switch (registeredEvent.event()) {

      // TODO: also read other interesting events, such as:
      //  - Ping creation callbacks

      case Event.LuaSimCallback(
        String func, LuaData.Table callbackTable, Event.CommandUnits commandUnits
      ) when func.equals("ModeratorEvent") -> {

        String playerNameFromCommandSource = registeredEvent.source().name();
        Integer activeCommandSource = registeredEvent.source().sourceId();

        String messageContent = null;
        String playerNameFromArmy = null;
        Integer fromArmy = null;

        // This fields only exists to function as a trap - it doesn't actually affect the messaging even though it appears it does so in-game
        if ((callbackTable.value().get("From") instanceof LuaData.Number from)) {

          // focus army starts is 1-based instead of 0-based, to align it we subtract 1
          fromArmy = (int) from.value() - 1;

          if (fromArmy != -2) {
            Source source = sources.get(fromArmy);

            if (source != null) {
              playerNameFromArmy = (String) source.name();
            }
          }
        }

        if ((callbackTable.value().get("Message") instanceof LuaData.String content)) {
          messageContent = content.value();
        }

        yield new ModeratorEvent(tickToDuration(registeredEvent.tick()), activeCommandSource, fromArmy, messageContent, playerNameFromArmy, playerNameFromCommandSource);
      }
      default -> null;
    }).filter(Objects::nonNull).toList();
  }
}
