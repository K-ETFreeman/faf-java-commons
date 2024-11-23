package com.faforever.commons.io;

@FunctionalInterface
public interface ByteCountListener {

  void updateBytesProcessed(long processed, long total);
}
