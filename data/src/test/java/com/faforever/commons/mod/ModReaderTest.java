package com.faforever.commons.mod;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModReaderTest {

  @TempDir
  public Path temporaryFolder;

  private ModReader instance;

  @BeforeEach
  public void setUp() {
    instance = new ModReader();
  }

  @Test
  public void testModReadInfo() throws IOException {
    Path modPath = temporaryFolder.resolve("mod-with-url");
    Path modInfoPath = temporaryFolder.resolve("mod-with-url/mod_info.lua");
    Files.createDirectory(modPath);
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/mod/mod-with-url.lua")), modInfoPath);

    Mod mod = instance.readDirectory(modPath);

    assertEquals("https://github.com/JeroenDeDauw/NoAirCrashDamage", mod.getUrl());
  }

  @Test
  public void testRegularMod() throws Exception {
    assertEquals("good", instance.readZip(prepareRegularMod()).getName());
  }

  @Test
  public void testMissingModInfo() {
    ModLoadException ex = assertThrows(ModLoadException.class, () -> instance.readZip(prepareMissingModInfo()));
    assertThat(ex.getMessage(), startsWith("Missing mod_info.lua in:"));
  }

  @Test
  public void testModPack() throws Exception {
    assertEquals("good", instance.readZip(prepareModPack()).getName());
  }

  @NotNull
  private Path prepareRegularMod() throws IOException {
    Path uploadFile = temporaryFolder.resolve("regular-mod.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(uploadFile))) {
      zos.putNextEntry(new ZipEntry("foobar/mod_info.png"));
      zos.putNextEntry(new ZipEntry("foobar/mod_info.lua"));
      zos.write("name = \"good\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/hook/lua/sim/Unit.lua"));
      zos.write("name = \"unit\"".getBytes());
      zos.closeEntry();
    }
    return uploadFile;
  }


  @NotNull
  private Path prepareMissingModInfo() throws IOException {
    Path uploadFile = temporaryFolder.resolve("regular-mod.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(uploadFile))) {
      zos.putNextEntry(new ZipEntry("foobar/mod_info.png"));
      zos.putNextEntry(new ZipEntry("foobar/MOD_INFO_TYPO.lua"));
      zos.write("name = \"good\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/hook/lua/sim/Unit.lua"));
      zos.write("name = \"unit\"".getBytes());
      zos.closeEntry();
    }
    return uploadFile;
  }

  @NotNull
  private Path prepareModPack() throws IOException {
    Path uploadFile = temporaryFolder.resolve("mod-pack.zip");
    try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(uploadFile))) {
      zos.putNextEntry(new ZipEntry("foobar/mod_info.png"));
      zos.putNextEntry(new ZipEntry("foobar/mod_info.lua"));
      zos.write("name = \"good\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/a/lua/sim/mod_info.lua"));
      zos.write("name = \"bad-a\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/b/lua/sim/mod_info.lua"));
      zos.write("name = \"bad-b\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/c/lua/sim/mod_info.lua"));
      zos.write("name = \"bad-c\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/d/lua/sim/mod_info.lua"));
      zos.write("name = \"bad-d\"".getBytes());
      zos.putNextEntry(new ZipEntry("foobar/e/lua/sim/mod_info.lua"));
      zos.write("name = \"bad-e\"".getBytes());
      zos.closeEntry();
    }
    return uploadFile;
  }

}
