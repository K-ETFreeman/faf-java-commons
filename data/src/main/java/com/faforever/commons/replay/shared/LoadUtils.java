package com.faforever.commons.replay.shared;

import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LoadUtils {

  @Contract(pure = true)
  private static int peek(ByteBuffer buffer) {
    buffer.mark();
    int next = getUnsignedByte(buffer);
    buffer.reset();
    return next;
  }

  /**
   * Parses a value from the data stream
   *
   * @param buffer
   * @return
   */
  @Contract(pure = true)
  public static String readString(ByteBuffer buffer){
    final int offset = buffer.position();
    while (buffer.get() != 0) {
    }
    final int length = buffer.position() - 1 - offset;
    byte[] stringBytes = new byte[length];
    buffer.get(offset, stringBytes, 0, length);
    return new String(stringBytes, StandardCharsets.UTF_8);
  }

  /**
   * Parses a Lua table from the data stream
   *
   * @param buffer
   * @return
   */
  @Contract(pure = true)
  public static LuaData parseLua(ByteBuffer buffer) {
    int type = getUnsignedByte(buffer);

    final int LUA_NUMBER = 0;
    final int LUA_STRING = 1;
    final int LUA_NIL = 2;
    final int LUA_BOOL = 3;
    final int LUA_TABLE_START = 4;
    final int LUA_TABLE_END = 5;

    switch (type) {
      case LUA_NUMBER -> {
        float value = buffer.getFloat();
        return new LuaData.Number(value);
      }

      case LUA_STRING -> {
        String value = readString(buffer);
        return new LuaData.String(value);
      }

      case LUA_NIL -> {
        return new LuaData.Nil();
      }

      case LUA_BOOL -> {
        boolean value = getUnsignedByte(buffer) == 0;
        return new LuaData.Bool(value);
      }

      case LUA_TABLE_START -> {
        Map<String, LuaData> value = new HashMap<>();
        while (peek(buffer) != LUA_TABLE_END) {
          LuaData key = parseLua(buffer);

          switch (key) {
            case LuaData.String(String str) -> value.put(str, parseLua(buffer));

            case LuaData.Number(float num) -> value.put(String.valueOf(num), parseLua(buffer));

            default -> throw new IllegalStateException("Unexpected data type: " + type);
          }

          buffer.mark();
        }
        buffer.get();

        return new LuaData.Table(value);
      }
      default -> throw new IllegalStateException("Unexpected data type: " + type);
    }
  }

  public static int getUnsignedByte(ByteBuffer buffer) {
    return buffer.get() & 0xFF;
  }
}
