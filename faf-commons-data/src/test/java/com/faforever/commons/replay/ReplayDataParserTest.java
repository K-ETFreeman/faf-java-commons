package com.faforever.commons.replay;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.LittleEndianDataInputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class ReplayDataParserTest {

  @TempDir
  public Path temporaryFolder;

  private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  @Test
  public void parse() throws Exception {
    Path replayFile = temporaryFolder.resolve("tmp.fafreplay");
    Files.copy(getClass().getResourceAsStream("/replay/test.fafreplay"), replayFile);

    ReplayDataParser parser = new ReplayDataParser(replayFile, objectMapper);

    List<GameOption> gameOptions = parser.getGameOptions();
    assertThat(gameOptions.size(), is(28));
    assertThat(gameOptions.get(0), is(new GameOption("CheatMult", "2.0")));
    assertThat(gameOptions.get(1), is(new GameOption("Timeouts", "3")));
    assertThat(gameOptions.get(10), is(new GameOption("ScenarioFile", "/maps/scmp_009/scmp_009_scenario.lua")));
    assertThat(gameOptions.get(12), is(new GameOption("ClanTags", ImmutableMap.builder()
      .put("Geosearchef", "")
      .put("Tokyto", "e")
      .put("Jitsu", "JEW")
      .put("kubkolienka", "")
      .put("sandwormsurfer", "GDI")
      .put("MazorNoob", "SNF")
      .put("Nojoke", "TEA")
      .put("hepko", "e")
      .build()
    )));

    List<ChatMessage> chatMessages = parser.getChatMessages();
    assertThat(chatMessages.get(0), is(new ChatMessage(Duration.ofSeconds(30), "kubkolienka", "all", "how does this game play?:D")));
  }

  @Test
  void testReadString() throws Exception {
    String unicodeString = "Oh, helloäöüthere!";

    byte[] stringBytes = (unicodeString + "\0").getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream byteInputStream = new ByteArrayInputStream(stringBytes);
    LittleEndianDataInputStream dataInputStream = new LittleEndianDataInputStream(byteInputStream);

    String result = ReplayDataParser.readString(dataInputStream);

    assertThat(result, is(unicodeString));
  }

  private void testBadReplay(String name) throws Exception {
    Path replayFile = temporaryFolder.resolve("tmp.fafreplay");
    Files.copy(getClass().getResourceAsStream(String.format("/replay/%s.fafreplay", name)), replayFile);
    Assertions.assertThrows(Exception.class, () -> new ReplayDataParser(replayFile, objectMapper));
  }

  @Test
  public void testNoNewlineHeader() throws Exception {
    testBadReplay("bad_nonewline");
  }

  @Test
  public void testBadJsonHeader() throws Exception {
    testBadReplay("bad_json");
  }

  @Test
  public void testBadCompressionType() throws Exception {
    testBadReplay("bad_compression_type");
  }

  @Test
  public void testUnknownCompression() throws Exception {
    testBadReplay("bad_unknown_compression");
  }

  @Test
  public void testBadBase64() throws Exception {
    testBadReplay("bad_base64");
  }

  @Test
  public void testReferenceZstd() throws Exception {
    Path replayFile = temporaryFolder.resolve("tmp.fafreplay");
    Path referenceFile = temporaryFolder.resolve("reference.fafreplay");
    Files.copy(getClass().getResourceAsStream("/replay/zstd_reference.fafreplay"), replayFile);
    Files.copy(getClass().getResourceAsStream("/replay/zstd_reference.raw"), referenceFile);

    byte[] data = new ReplayDataParser(replayFile, objectMapper).getData();
    byte[] reference = Files.readAllBytes(referenceFile);
    assertThat("Zstd compressed replay matches reference", Arrays.equals(data, reference));
  }

  @Test
  public void testLegacyFormat() throws Exception {
    Path replayFile = temporaryFolder.resolve("test.fafreplay");
    Path referenceFile = temporaryFolder.resolve("test.raw");
    Files.copy(getClass().getResourceAsStream("/replay/test.fafreplay"), replayFile);
    Files.copy(getClass().getResourceAsStream("/replay/test.raw"), referenceFile);

    byte[] data = new ReplayDataParser(replayFile, objectMapper).getData();
    byte[] reference = Files.readAllBytes(referenceFile);
    assertThat("Legacy compressed file matches reference", Arrays.equals(data, reference));
  }
}
