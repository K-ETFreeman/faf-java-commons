package com.faforever.commons.replay;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static com.faforever.commons.test.IsUtilityClassMatcher.isUtilityClass;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class QtCompressTest {

  public static final byte[] UNCOMPRESSED_BYTES = "AnyBytes".getBytes(StandardCharsets.US_ASCII);
  public static final byte[] COMPRESSED_BYTES = new byte[]{
      // Number of uncompressed bytes
      0, 0, 0, (byte) UNCOMPRESSED_BYTES.length,
      // Compressed bytes
      120, -100, 114, -52, -85, 116, -86, 44, 73, 45, 6, 0, 0, 0, -1, -1, 3, 0, 13, -81, 3, 48
  };

  @Test
  void testIsUtilityClass() {
    assertThat(QtCompress.class, isUtilityClass());
  }

  @Test
  void testQCompress() throws Exception {
    byte[] compressedBytes = QtCompress.qCompress(UNCOMPRESSED_BYTES);
    assertArrayEquals(COMPRESSED_BYTES, compressedBytes);
  }

  @Test
  void testQUncompress() throws Exception {
    byte[] uncompressedBytes = QtCompress.qUncompress(COMPRESSED_BYTES);
    assertArrayEquals(UNCOMPRESSED_BYTES, uncompressedBytes);
  }
}
