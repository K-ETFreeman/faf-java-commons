package com.faforever.commons.replay;

import java.time.Duration;

public record ModeratorEvent(Duration time,
                             int activeCommandSource,
                             int fromArmy,
                             String message,
                             String playerNameFromArmy,
                             String playerNameFromCommandSource) {
}
