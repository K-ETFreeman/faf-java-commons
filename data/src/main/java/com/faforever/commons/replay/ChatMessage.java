package com.faforever.commons.replay;

import lombok.Data;

import java.time.Duration;

@Data
public class ChatMessage {

  private final Duration time;
  private final String sender;
  private final String receiver;
  private final String message;
}
