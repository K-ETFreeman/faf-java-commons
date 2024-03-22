package com.faforever.commons.replay;

import java.time.Duration;

public record ModeratorEvent(Duration time, String sender, String message, int activeCommandSource) {

}
