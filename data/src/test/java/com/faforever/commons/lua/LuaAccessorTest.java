package com.faforever.commons.lua;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

public class LuaAccessorTest {
  private LuaAccessor instance;

  @BeforeEach
  void beforeEach() throws Exception {
    Path mapScenarioLua = Paths.get(getClass().getResource("/lua/map_scenario.lua").toURI());
    instance = LuaAccessor.of(mapScenarioLua, "ScenarioInfo");
  }

  @Test
  void testEmptyLua() throws Exception {
    LuaError luaErrorMissingVersion = assertThrows(LuaError.class, () -> LuaAccessor.of("emptyCode", "root"));
    assertThat(luaErrorMissingVersion.getMessage(), containsString("syntax error"));

    Path emptyTxtPath = Paths.get(getClass().getResource("/lua/empty.txt").toURI());
    luaErrorMissingVersion = assertThrows(LuaError.class, () -> LuaAccessor.of(emptyTxtPath, "root"));
    assertThat(luaErrorMissingVersion.getMessage(), is("Lua version declaration is missing."));
  }

  @Test
  void testMissingVersionHeader() throws Exception {
    Path wrongVersionLuaPath = Paths.get(getClass().getResource("/lua/wrong_version.lua").toURI());
    LuaError error = assertThrows(LuaError.class, () -> LuaAccessor.of(wrongVersionLuaPath, "root"));
    assertThat(error.getMessage(), is("Unsupported lua version: only version 3 is supported"));
  }

  @Test
  void testWrongRootContext() throws Exception {
    Path mapScenarioLua = Paths.get(getClass().getResource("/lua/map_scenario.lua").toURI());
    LuaError luaError = assertThrows(LuaError.class, () -> LuaAccessor.of(mapScenarioLua, "wrongContext"));
    assertThat(luaError.getMessage(), is("Root element 'wrongContext' is not defined."));
  }

  @Test
  void testIsValue() {
    LuaValue configurations = instance.readVariable("Configurations").get();
    assertTrue(LuaAccessor.isValue(configurations));
    assertFalse(LuaAccessor.isValue(configurations, "nonExisting"));

    assertFalse(LuaAccessor.isValue(LuaValue.NIL));
  }

  @Test
  void testReadVariable() {
    assertThrows(IllegalArgumentException.class, () -> instance.readVariable());

    assertTrue(instance.readVariable("Configurations").isPresent());
    assertTrue(instance.readVariable("Configurations", "standard").isPresent());
    assertTrue(instance.readVariable("Configurations", "standard", "customprops").isPresent());
    assertFalse(instance.readVariable("Configurations", "standard", "customprops", "nonExisting").isPresent());
  }

  @Test
  void testReadVariableString() {
    assertThrows(IllegalArgumentException.class, () -> instance.readVariableString());

    assertEquals("skirmish", instance.readVariableString("type").get());
    assertEquals("50", instance.readVariableString("norushradius").get());
  }

  @Test
  void testReadVariableInt() {
    assertThrows(IllegalArgumentException.class, () -> instance.readVariableInt());

    assertEquals(0, instance.readVariableInt("type").getAsInt());
    assertEquals(50, instance.readVariableInt("norushradius").getAsInt());
  }

  @Test
  void testReadVariableBool() {
    assertThrows(IllegalArgumentException.class, () -> instance.readVariableBool());

    assertEquals(true, instance.readVariableBool("starts").get());
    assertEquals(false, instance.readVariableBool("negative_bool").get());
  }

  @Test
  void testHasVariableMatching() {
    assertThrows(IllegalArgumentException.class, () -> instance.hasVariableMatching("anyRegex"));

    assertTrue(instance.hasVariableMatching("skirmish", "type"));
    assertTrue(instance.hasVariableMatching(".*rmi.*", "type"));

    assertFalse(instance.hasVariableMatching("[]invalidPattern+*?*", "type"));
  }

  @Test
  void testHasVariableMatchingIgnoreCase() {
    assertThrows(IllegalArgumentException.class, () -> instance.hasVariableMatchingIgnoreCase("anyRegex"));

    assertTrue(instance.hasVariableMatchingIgnoreCase("FOR EXAMPLE.*", "description"));
    assertTrue(instance.hasVariableMatchingIgnoreCase("for example.*", "description"));

    assertFalse(instance.hasVariableMatchingIgnoreCase("[]invalidPattern+*?*", "type"));
  }
}
