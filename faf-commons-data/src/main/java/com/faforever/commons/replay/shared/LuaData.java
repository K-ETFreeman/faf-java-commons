package com.faforever.commons.replay.shared;

import java.util.Map;

public sealed interface LuaData {
  record Number(float value) implements LuaData {}
  record String(java.lang.String value) implements LuaData {}
  record Nil() implements LuaData {}
  record Table(Map<java.lang.String, LuaData> value) implements LuaData {}
  record Bool(boolean value) implements LuaData {}
}
