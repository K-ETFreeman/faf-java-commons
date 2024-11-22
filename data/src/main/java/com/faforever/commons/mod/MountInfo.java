package com.faforever.commons.mod;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;

@Getter
@RequiredArgsConstructor
public class MountInfo {

  /**
   * The local directory directory to resolve the {@link #file} against.
   */
  private final Path baseDir;

  /**
   * The ZIP file or directory on the local file system, relative to {@link #baseDir}, that should be mounted into the
   * virtual file system of the game.
   */
  private final Path file;

  /**
   * The absolute mount path (starting with {@code /}) within the game's virtual file system to which the directory
   * returned by {@link #getFile()} should be mounted to.
   */
  private final String mountPoint;
}
