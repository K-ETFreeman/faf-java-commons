package com.faforever.commons.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZipperAndUnzipperTest {

  @TempDir
  Path folderToZip;
  @TempDir
  Path targetFolder;
  @TempDir
  Path folderToUnzip;

  @Test
  void testZip() throws Exception {

    Path file1 = Files.createFile(folderToZip.resolve("file1"));
    Files.createFile(folderToZip.resolve("file2"));

    Path folder1 = Files.createDirectory(folderToZip.resolve("folder1"));
    Files.createFile(folder1.resolve("file1"));
    Path folder11 = Files.createDirectory(folder1.resolve("folder11"));
    Files.createFile(folder11.resolve("file1"));

    Path folder2 = Files.createDirectory(folderToZip.resolve("folder2"));
    Files.createFile(folder2.resolve("file1"));

    Files.createDirectory(folderToZip.resolve("folder3"));

    byte[] file1Contents = new byte[1024];
    new Random().nextBytes(file1Contents);

    Files.write(file1, file1Contents);

    Path zipFile = targetFolder.resolve("target.zip");
    Zipper.contentOf(folderToZip)
      .to(zipFile)
      .zip();

    Unzipper.from(zipFile)
      .to(targetFolder)
      .unzip();

    Path targetDirectory = targetFolder;

    assertTrue(Files.exists(targetDirectory.resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("file2")));
    assertTrue(Files.exists(targetDirectory.resolve("folder1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder1").resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder2")));
    assertTrue(Files.exists(targetDirectory.resolve("folder2").resolve("file1")));
    assertTrue(Files.exists(targetDirectory.resolve("folder3")));

    assertArrayEquals(file1Contents, Files.readAllBytes(targetDirectory.resolve("file1")));
  }

  @Test
  void unzipZipBomb() throws Exception {
    Path path = Paths.get(getClass().getResource("/zip/zip_bomb.zip").toURI());

    Unzipper unzipper = Unzipper
      .from(path)
      .to(targetFolder)
      .byteCountInterval(1);

    assertThrows(ZipBombException.class, unzipper::unzip);
  }

  @Test
  void testSlipZip() throws Exception {
    Path path = Paths.get(getClass().getResource("/zip/slip_zip.zip").toURI());

    Unzipper unzipper = Unzipper.from(path)
      .to(folderToUnzip);

    assertThrows(AccessDeniedException.class, unzipper::unzip);
  }

  @Test
  void testUnzip() throws Exception {
    Path path = Paths.get(getClass().getResource("/zip/normal_zip.zip").toURI());
    long inputFileSize = Files.size(path);

    ByteCountListener byteCountListener = mock(ByteCountListener.class);

    Unzipper.Results results = Unzipper.from(path)
      .to(folderToUnzip)
      .listener(byteCountListener)
      .bufferSize(150)
      .unzip();


    Path file1 = folderToUnzip.resolve("example.txt");
    Path file2 = folderToUnzip.resolve("second_file.txt");
    assertThat(Files.exists(file1), is(true));
    assertThat(Files.exists(file2), is(true));

    long outputFileSize = Files.size(file1) + Files.size(file2);

    assertAll(
      () -> assertThat(inputFileSize, is(results.getTotalBytesRead())),
      () -> assertThat(outputFileSize, is(results.getTotalBytesWritten())),
      () -> assertThat(results.getFilesExtracted(), is(2))
    );

    verify(byteCountListener, atLeastOnce()).updateBytesProcessed(300, 300);
    verifyNoMoreInteractions(byteCountListener);
  }

  @Test
  void testUnzipWithoutListener() throws Exception {
    Path path = Paths.get(getClass().getResource("/zip/normal_zip.zip").toURI());

    Unzipper.from(path)
      .to(folderToUnzip)
      .unzip();

    assertThat(Files.exists(folderToUnzip.resolve("example.txt")), is(true));
    assertThat(Files.exists(folderToUnzip.resolve("second_file.txt")), is(true));
  }

  @Test
  void testUnzipWithDifferentBufferSize() throws Exception {
    Path path = Paths.get(getClass().getResource("/zip/normal_zip.zip").toURI());

    ByteCountListener byteCountListener = mock(ByteCountListener.class);

    Unzipper.from(path)
      .to(folderToUnzip)
      .listener(byteCountListener)
      .bufferSize(50)
      .unzip();

    assertThat(Files.exists(folderToUnzip.resolve("example.txt")), is(true));
    assertThat(Files.exists(folderToUnzip.resolve("second_file.txt")), is(true));

    verify(byteCountListener, atLeastOnce()).updateBytesProcessed(300, 300);
    verifyNoMoreInteractions(byteCountListener);
  }
}
