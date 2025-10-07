package com.faforever.commons.replay;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.*;

/**
 * Utility class that compresses and uncompresses bytes like QT's <a href="http://doc.qt.io/qt-5/qbytearray.html">QByteArray</a>.
 */
public final class QtCompress {

  private QtCompress() {
    throw new AssertionError("Not instantiatable");
  }

  /**
   * Compresses the specified bytes like <a href="http://doc.qt.io/qt-5/qbytearray.html#qCompress">QByteArray.qCompress()</a>
   * does.
   */
  public static ByteBuffer qUncompress(ByteBuffer inputBuffer) {
    Inflater inflater = new Inflater();
    final int uncompressedLength = inputBuffer.getInt();
    inflater.setInput(inputBuffer);
    final var outputBytes = new byte[uncompressedLength];

    try {
      inflater.inflate(outputBytes);
    } catch (DataFormatException e) {
      throw new RuntimeException(e);
    }

    return ByteBuffer.wrap(outputBytes);
  }

  /**
   * Uncompresses the specified bytes like <a href="http://doc.qt.io/qt-5/qbytearray.html#qCompress">QByteArray.qCompress()</a>
   * does.
   */
  public static byte[] qCompress(byte[] bytes) throws IOException {
    Deflater deflater = new Deflater();
    deflater.setInput(bytes);

    ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
    new DataOutputStream(byteArray).writeInt(bytes.length);

    try (DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(byteArray, deflater, true)) {
      deflaterOutputStream.flush();
    }

    return byteArray.toByteArray();
  }
}
