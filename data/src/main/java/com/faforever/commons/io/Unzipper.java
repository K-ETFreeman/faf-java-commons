package com.faforever.commons.io;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.CountingInputStream;
import org.apache.commons.compress.utils.CountingOutputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.*;

@Slf4j
public final class Unzipper {
  public Results unzip() throws IOException, ArchiveException {
    try (CountingInputStream countingInputStream = new CountingInputStream(inputStream);
         BufferedInputStream bufferedInputStream = new BufferedInputStream(countingInputStream, bufferSize);
         ArchiveInputStream archiveInputStream = new ArchiveStreamFactory()
           .createArchiveInputStream(bufferedInputStream)) {
      ArchiveEntry archiveEntry;
      long outputFilesBytesWritten = 0;
      int filesExtracted = 0;
      while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
        Path entryPath = Paths.get(archiveEntry.getName()).normalize();

        if (entryPath.startsWith("/") || entryPath.startsWith("..")) {
          throw new ZipSlipException("Zip slip content detected. Aborting unzip process");
        }

        Path targetFile = targetDirectory.resolve(entryPath);

        if (archiveEntry.isDirectory()) {
          log.trace("Creating directory {}", targetFile);
          Files.createDirectories(targetFile);
          continue;
        }

        Path parentDirectory = targetFile.getParent();
        if (Files.notExists(parentDirectory)) {
          log.trace("Creating directory {}", parentDirectory);
          Files.createDirectories(parentDirectory);
        }

        log.trace("Writing file {}", targetFile);
        try (OutputStream fileOutputStream = Files.newOutputStream(targetFile, CREATE, TRUNCATE_EXISTING, WRITE);
             CountingOutputStream countingOutputStream = new CountingOutputStream(fileOutputStream)) {
          final long currentOutputFilesBytesWritten = outputFilesBytesWritten;

          ByteCopier
            .from(archiveInputStream)
            .to(countingOutputStream)
            .bufferSize(bufferSize)
            .byteCountInterval(byteCountInterval)
            .totalBytes(totalBytes)
            .listener((written, total) -> zipBombCheck(countingInputStream.getBytesRead(), currentOutputFilesBytesWritten + countingOutputStream.getBytesWritten()))
            .copy();

          if (byteCountListener != null) {
            byteCountListener.updateBytesProcessed(countingInputStream.getBytesRead(), totalBytes);
          }

          outputFilesBytesWritten += countingOutputStream.getBytesWritten();
          filesExtracted++;

        }
      }

      return new Results(countingInputStream.getBytesRead(), outputFilesBytesWritten, filesExtracted);
    } finally {
      if (closeStream) {
        inputStream.close();
      }
    }
  }


  private final InputStream inputStream;
  private final boolean closeStream;

  private ByteCountListener byteCountListener;
  private int byteCountInterval;
  private int bufferSize;
  private long totalBytes;
  private Path targetDirectory;
  /**
   * Total amount of written bytes after which zip bomb protection goes active
   */
  private long zipBombByteCountThreshold;
  /**
   * Assume zip bomb if [read files from zip file] / [written bytes into stream] > [factor]
   */
  private int zipBombProtectionFactor;

  private Unzipper(InputStream inputStream, boolean closeStream) {
    this.inputStream = inputStream;
    this.closeStream = closeStream;
    // 4K
    bufferSize = 0x1000;
    byteCountInterval = 40;
    zipBombByteCountThreshold = 1_000_000;
    zipBombProtectionFactor = 100;
  }

  public static Unzipper from(Path zipFile) throws IOException {
    return new Unzipper(Files.newInputStream(zipFile), true)
      .totalBytes(Files.size(zipFile));
  }

  public static Unzipper from(InputStream inputStream) {
    return new Unzipper(inputStream, false);
  }

  public Unzipper to(Path targetDirectory) {
    this.targetDirectory = targetDirectory;
    return this;
  }

  public Unzipper byteCountInterval(int byteCountInterval) {
    this.byteCountInterval = byteCountInterval;
    return this;
  }

  public Unzipper listener(ByteCountListener byteCountListener) {
    this.byteCountListener = byteCountListener;
    return this;
  }

  public Unzipper bufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public Unzipper totalBytes(long totalBytes) {
    this.totalBytes = totalBytes;
    return this;
  }

  public Unzipper zipBombByteCountThreshold(long threshold) {
    this.zipBombByteCountThreshold = threshold;
    return this;
  }

  public Unzipper zipBombProtectionFactor(int factor) {
    this.zipBombProtectionFactor = factor;
    return this;
  }

  @Value
  public class Results {
    long totalBytesRead;
    long totalBytesWritten;
    int filesExtracted;
  }

  private void zipBombCheck(long inputBytesRead, long outputBytesWritten) {
    if (outputBytesWritten > zipBombByteCountThreshold
      && outputBytesWritten / inputBytesRead > zipBombProtectionFactor) {
      throw new ZipBombException("Zip bomb detected. Aborting unzip process");
    }
  }
}
