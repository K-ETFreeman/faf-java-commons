package com.faforever.commons.fa;

import com.google.common.primitives.Ints;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;

@Slf4j
@UtilityClass
public final class ForgedAllianceExePatcher {

  private static final int[] VERSION_ADDRESSES = new int[]{0xd3d40, 0x47612d, 0x476666};

  @SneakyThrows
  public void patchVersion(Path exePath, int version) {
    byte[] versionAsLittleEndianBytes = toLittleEndianByteArray(version);
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(exePath.toFile(), "rw")) {
      log.debug("Updating version in {} to {}", exePath, version);
      for (int versionAddress : VERSION_ADDRESSES) {
        randomAccessFile.seek(versionAddress);
        randomAccessFile.write(versionAsLittleEndianBytes);
      }
    }
  }

  @SneakyThrows
  public int readVersion(Path exePath) {
    try (RandomAccessFile randomAccessFile = new RandomAccessFile(exePath.toFile(), "r")) {
      randomAccessFile.seek(VERSION_ADDRESSES[0]);
      return fromLittleEndian(randomAccessFile.readInt());
    }
  }

  private static byte[] toLittleEndianByteArray(int i) {
    return ByteBuffer.allocate(Integer.SIZE / Byte.SIZE).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array();
  }

  private static int fromLittleEndian(int i) {
    byte b1 = (byte) (0xFF & i >> 24);
    byte b2 = (byte) (0xFF & i >> 16);
    byte b3 = (byte) (0xFF & i >> 8);
    byte b4 = (byte) (0xFF & i);

    return Ints.fromBytes(b4, b3, b2, b1);
  }
}
