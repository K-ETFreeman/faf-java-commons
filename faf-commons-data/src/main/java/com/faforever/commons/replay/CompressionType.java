package com.faforever.commons.replay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum CompressionType {
  /**
   * Base64 encoded deflate compressed stream
   */
  QTCOMPRESS(null),
  /**
   * Zstandard compressed stream
   */
  ZSTD("zstd"),
  UNKNOWN("unknown");

  private static final Map<String, CompressionType> fromString;

  static {
    fromString = new HashMap<>(values().length, 1);
    for (CompressionType compressionType : values()) {
      fromString.put(compressionType.getString(), compressionType);
    }
  }

  private final String string;

  public static CompressionType fromString(String string) {
    return fromString.getOrDefault(string, CompressionType.UNKNOWN);
  }
}
