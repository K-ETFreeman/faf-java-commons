package com.faforever.commons.io;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.CountingOutputStream;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

@Slf4j
public final class Zipper {

  private static final char PATH_SEPARATOR = File.separatorChar;
  private static final Path NEUTRAL_PATH = Paths.get("");

  private final Path directoryToZip;
  private final boolean zipContent;
  private final String archiveType;

  private ByteCountListener byteCountListener;
  private int byteCountInterval;
  private int bufferSize;
  private long bytesTotal;
  private long bytesDone;
  private OutputStream outputStream;
  private boolean closeStream;

  /**
   * @param zipContent {@code true} if the contents of the directory should be zipped, {@code false} if the specified
   */
  private Zipper(Path directoryToZip, boolean zipContent, String archiveType) {
    this.directoryToZip = directoryToZip;
    this.archiveType = archiveType;
    this.zipContent = zipContent;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 40;
  }

  public static Zipper of(Path path) {
    return of(path, ArchiveStreamFactory.ZIP);
  }

  public static Zipper of(Path path, String archiveType) {
    return new Zipper(path, false, archiveType);
  }

  public static Zipper contentOf(Path path) {
    return contentOf(path, ArchiveStreamFactory.ZIP);
  }

  public static Zipper contentOf(Path path, String archiveType) {
    return new Zipper(path, true, archiveType);
  }

  public Zipper to(OutputStream outputStream) {
    this.outputStream = outputStream;
    this.closeStream = false;
    return this;
  }

  public Zipper to(Path path) throws IOException {
    this.outputStream = Files.newOutputStream(path);
    this.closeStream = true;
    return this;
  }

  public Zipper byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public Zipper listener(ByteCountListener byteCountListener) {
    this.byteCountListener = byteCountListener;
    return this;
  }

  public void zip() throws IOException, ArchiveException {
    Objects.requireNonNull(outputStream, "outputStream must not be null");
    Objects.requireNonNull(directoryToZip, "directoryToZip must not be null");

    bytesTotal = calculateTotalBytes();
    bytesDone = 0;

    try (
      CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
      BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(countingOutputStream);
      ArchiveOutputStream archiveOutputStream = new ArchiveStreamFactory()
        .createArchiveOutputStream(archiveType, bufferedOutputStream)) {

      Files.walkFileTree(directoryToZip, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          Path relativePath = zipContent
            ? directoryToZip.relativize(dir)
            : directoryToZip.getParent().relativize(dir);

          if (relativePath.equals(NEUTRAL_PATH)) {
            return FileVisitResult.CONTINUE;
          }

          ArchiveEntry archiveEntry = archiveOutputStream.createArchiveEntry(dir.toFile(), relativePath.toString().replace(PATH_SEPARATOR, '/') + '/');
          archiveOutputStream.putArchiveEntry(archiveEntry);
          archiveOutputStream.closeArchiveEntry();
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          log.trace("Zipping file {}", file.toAbsolutePath());

          Path relativePath = zipContent
            ? directoryToZip.relativize(file)
            : directoryToZip.getParent().relativize(file);

          ArchiveEntry archiveEntry = archiveOutputStream.createArchiveEntry(file.toFile(), relativePath.toString().replace(PATH_SEPARATOR, '/'));
          archiveOutputStream.putArchiveEntry(archiveEntry);

          try (InputStream inputStream = Files.newInputStream(file)) {
            final long currentBytesDone = bytesDone;
            ByteCopier
              .from(inputStream)
              .to(archiveOutputStream)
              .bufferSize(bufferSize)
              .byteCountInterval(byteCountInterval)
              .totalBytes(bytesTotal)
              .listener((written, total) -> updateBytesCounted(currentBytesDone + written, total))
              .copy();
          }

          archiveOutputStream.closeArchiveEntry();
          bytesDone = countingOutputStream.getBytesWritten();

          return FileVisitResult.CONTINUE;
        }
      });
    } finally {
      if (closeStream) {
        outputStream.close();
      }
    }
  }

  private void updateBytesCounted(long written, long total) {
    if (byteCountListener == null)
      return;

    byteCountListener.updateBytesProcessed(written, total);
  }

  private long calculateTotalBytes() throws IOException {
    final long[] currentBytes = {0};
    Files.walkFileTree(directoryToZip, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        currentBytes[0] += Files.size(file);
        return FileVisitResult.CONTINUE;
      }
    });
    return currentBytes[0];
  }
}
