package com.faforever.commons.replay.shared;

import java.util.HashMap;
import java.util.Map;

public sealed interface LuaData {
  record Nil() implements LuaData {}
  record Number(float value) implements LuaData {}
  record String(java.lang.String value) implements LuaData {}
  record Bool(boolean value) implements LuaData {}

  record Table(Map<java.lang.String, LuaData> value) implements LuaData {

    public void removeKey(java.lang.String key) {
      value.remove(key);
    }

    public Boolean getBool(java.lang.String key) {
      if (value.get(key) instanceof LuaData.Bool (boolean bool)) {
        return bool;
      }

      return null;
    }

    public java.lang.String getString(java.lang.String key) {
      if (value.get(key) instanceof LuaData.String (java.lang.String string)) {
        return string;
      }

      return null;
    }

    public Float getFloat(java.lang.String key) {
      if (value.get(key) instanceof LuaData.Number (float number)) {
        return number;
      }

      return null;
    }

    public Integer getInteger(java.lang.String key) {
      if (value.get(key) instanceof LuaData.Number (float number)) {
        return  (Integer) (int) Math.floor(number);
      }

      return null;
    }

    public LuaData.Table getTable(java.lang.String key) {
      if (value.get(key) instanceof LuaData.Table table) {
        return  table;
      }

      return null;
    }

    public Map<java.lang.String, java.lang.String> toKeyStringValuePairs() {
      HashMap<java.lang.String, java.lang.String> map = new HashMap<java.lang.String, java.lang.String>();

      value.forEach(
        (k, v) -> {
          switch (v) {
            case LuaData.Nil e -> {
              map.put(k, "nil");
            }
            case Bool bool -> {
              map.put(k, java.lang.String.valueOf(bool.value()));
            }
            case Number number -> {
              map.put(k, java.lang.String.valueOf(number.value()));
            }
            case String string -> {
              map.put(k, java.lang.String.valueOf(string.value()));
            }
            case Table table -> {
              map.put(k, java.lang.String.valueOf("table"));
            }
          }
        }
      );

      return map;
    }
  }
}
