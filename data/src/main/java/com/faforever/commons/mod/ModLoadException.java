package com.faforever.commons.mod;

public class ModLoadException extends RuntimeException {
  public ModLoadException(Throwable cause) {
    super(cause);
  }

  public ModLoadException(String message) {
    super(message);
  }
}
