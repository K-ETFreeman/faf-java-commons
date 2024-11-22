package com.faforever.commons.fa;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ForgedAllianceExePatcherTest {

  @TempDir
  Path fafBinDirectory;

  @Test
  void patchVersion() throws Exception {
    Path dummyExe = createDummyExeFile();
    ForgedAllianceExePatcher.patchVersion(dummyExe, 3660);

    assertThat(Files.hash(dummyExe.toFile(), Hashing.md5()).toString(), is("4de5eed29b45b640fe64aa22808631c3"));
  }

  private Path createDummyExeFile() throws IOException {
    Path dummyExe = fafBinDirectory.resolve("ForgedAlliance.exe");
    createFileWithSize(dummyExe, 12_444_928);
    return dummyExe;
  }

  @Test
  void readVersion() throws Exception {
    Path dummyExe = createDummyExeFile();
    ForgedAllianceExePatcher.patchVersion(dummyExe, 3660);

    int result = ForgedAllianceExePatcher.readVersion(dummyExe);
    assertThat(result, is(3660));
  }

  private void createFileWithSize(Path file, int size) throws IOException {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rw")) {
      randomAccessFile.setLength(size);
    }
  }
}
