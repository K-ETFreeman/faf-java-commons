package com.faforever.commons.lua;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.PatternSyntaxException;

import static java.text.MessageFormat.format;

/**
 * A utility class to safely read values from lua code
 */
public class LuaAccessor {
  private final static String VERSION = "version";

  private final LuaValue rootContext;

  private LuaAccessor(LuaValue rootContext) {
    this.rootContext = rootContext;
  }

  private static LuaAccessor of(LuaValue rootContext, String rootElement) {
    if (!isValue(rootContext, VERSION)) {
      throw new LuaError("Lua version declaration is missing.");
    } else if (rootContext.get(VERSION).toint() != 3) {
      throw new LuaError("Unsupported lua version: only version 3 is supported");
    }

    if (!isValue(rootContext, rootElement)) {
      throw new LuaError(format("Root element ''{0}'' is not defined.", rootElement));
    }

    return new LuaAccessor(rootContext.get(rootElement));
  }

  public static LuaAccessor of(Path luaPath, String rootElement) throws IOException {
    LuaValue rootContext = LuaLoader.loadFile(luaPath);
    return of(rootContext, rootElement);
  }

  public static LuaAccessor of(String luaCode, String rootElement) throws IOException {
    LuaValue rootContext = LuaLoader.load(luaCode);
    return of(rootContext, rootElement);
  }

  public static boolean isValue(LuaValue parent, String name) {
    LuaValue value = parent.get(name);
    return isValue(value);
  }

  public static boolean isValue(LuaValue value) {
    return value != LuaValue.NIL && !(value instanceof LuaFunction);
  }

  public Optional<LuaValue> readVariable(LuaValue parent, String... names) {
    if (names.length < 1) {
      throw new IllegalArgumentException("At least one variable name must be given!");
    }

    if (!isValue(parent, names[0])) {
      return Optional.empty();
    }

    LuaValue nextElement = parent.get(names[0]);

    if (names.length == 1) {
      return Optional.of(nextElement);
    } else {
      return readVariable(nextElement, Arrays.copyOfRange(names, 1, names.length));
    }
  }

  public Optional<LuaValue> readVariable(String... names) {
    return readVariable(rootContext, names);
  }

  public Optional<String> readVariableString(String... names) {
    return readVariable(rootContext, names)
      .map(LuaValue::tojstring);
  }

  public OptionalInt readVariableInt(String... names) {
    return readVariable(rootContext, names)
      .map(value -> OptionalInt.of(value.toint()))
      .orElseGet(OptionalInt::empty);
  }

  public Optional<Boolean> readVariableBool(String... names) {
    return readVariable(rootContext, names)
      .map(LuaValue::toboolean);
  }

  public boolean hasVariableMatching(String regex, String... names) {
    return readVariableString(names)
      .map(string -> {
        try {
          return string.matches(regex);
        } catch (PatternSyntaxException e) {
          return false;
        }
      })
      .orElse(false);
  }

  public boolean hasVariableMatchingIgnoreCase(String regex, String... names) {
    return readVariableString(names)
      .map(string -> {
        try {
          return string.toLowerCase().matches(regex.toLowerCase());
        } catch (PatternSyntaxException e) {
          return false;
        }
      })
      .orElse(false);
  }
}
