package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class LoadReplayLoaderTest {

  @TempDir
  public Path temporaryFolder;

  private final ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  private void assertNoUnprocessedTokens(ReplayContainer fafReplayContainer) {
    assertThat("No unprocessed tokens", fafReplayContainer.registeredEvents().stream().filter(e -> e.event() instanceof Event.Unprocessed).findAny().isEmpty());
  }

  private void assertNoErrorTokens(ReplayContainer fafReplayContainer) {
    assertThat("No error tokens", fafReplayContainer.registeredEvents().stream().filter(e -> e.event() instanceof Event.ProcessingError).findAny().isEmpty());
  }

  @Test
  public void parseBinary01() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("TestCommands01.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/TestCommands01.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(106, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary02() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("TestModeratorEvents.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/TestModeratorEvents.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(2, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary03() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("zstd_reference.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/zstd_reference.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(1, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary04() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("test.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/test.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(7, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary05() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("22451957.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22451957.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(0, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary06() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("22453414.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22453414.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(0, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary07() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("22453511.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22453511.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(3, chatMessages.size());
      }
    );
  }

  @Test
  public void parseBinary08() throws CompressorException, IOException {
    assertDoesNotThrow(
      () -> {
        Path fafReplayFile = temporaryFolder.resolve("mods.fafreplay");
        Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/mods.fafreplay")), fafReplayFile);
        ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);

        assertEquals(2, fafReplayContainer.header().mods().size());

        assertNoUnprocessedTokens(fafReplayContainer);
        assertNoErrorTokens(fafReplayContainer);

        List<ChatMessage> chatMessages = ReplaySemantics.findChatMessages(fafReplayContainer.header().sources(), fafReplayContainer.registeredEvents());
        assertEquals(0, chatMessages.size());
      }
    );
  }

  @Test
  public void compareBinary01() throws CompressorException, IOException {
    Path fafReplayFile = temporaryFolder.resolve("22338092.fafreplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22338092.fafreplay")), fafReplayFile);

    Path scfaReplayFile = temporaryFolder.resolve("22338092.scfareplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22338092.scfareplay")), scfaReplayFile);

    ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);
    assertNoUnprocessedTokens(fafReplayContainer);
    assertNoErrorTokens(fafReplayContainer);

    ReplayContainer scfaReplayContainer = ReplayLoader.loadSCFAReplayFromDisk(scfaReplayFile);
    assertNoUnprocessedTokens(scfaReplayContainer);
    assertNoErrorTokens(scfaReplayContainer);

    assertEquals(scfaReplayContainer.registeredEvents().size(), fafReplayContainer.registeredEvents().size());
    assertArrayEquals( scfaReplayContainer.registeredEvents().toArray(), fafReplayContainer.registeredEvents().toArray());
  }

  @Test
  public void compareBinary02() throws CompressorException, IOException {
    Path fafReplayFile = temporaryFolder.resolve("22373098.fafreplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22373098.fafreplay")), fafReplayFile);

    Path scfaReplayFile = temporaryFolder.resolve("22373098.scfareplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22373098.scfareplay")), scfaReplayFile);

    ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);
    assertNoUnprocessedTokens(fafReplayContainer);
    assertNoErrorTokens(fafReplayContainer);

    ReplayContainer scfaReplayContainer = ReplayLoader.loadSCFAReplayFromDisk(scfaReplayFile);
    assertNoUnprocessedTokens(scfaReplayContainer);
    assertNoErrorTokens(scfaReplayContainer);

    assertEquals(scfaReplayContainer.registeredEvents().size(), fafReplayContainer.registeredEvents().size());
    assertArrayEquals( scfaReplayContainer.registeredEvents().toArray(), fafReplayContainer.registeredEvents().toArray());

  }

  @Test
  public void compareBinary03() throws CompressorException, IOException {
    Path fafReplayFile = temporaryFolder.resolve("22425616.fafreplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22425616.fafreplay")), fafReplayFile);

    Path scfaReplayFile = temporaryFolder.resolve("22425616.scfareplay");
    Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/replay/load/22425616.scfareplay")), scfaReplayFile);

    ReplayContainer fafReplayContainer = ReplayLoader.loadFAFReplayFromDisk(fafReplayFile);
    assertNoUnprocessedTokens(fafReplayContainer);
    assertNoErrorTokens(fafReplayContainer);

    ReplayContainer scfaReplayContainer = ReplayLoader.loadSCFAReplayFromDisk(scfaReplayFile);
    assertNoUnprocessedTokens(scfaReplayContainer);
    assertNoErrorTokens(scfaReplayContainer);

    assertEquals(scfaReplayContainer.registeredEvents().size(), fafReplayContainer.registeredEvents().size());
    assertArrayEquals( scfaReplayContainer.registeredEvents().toArray(), fafReplayContainer.registeredEvents().toArray());
  }
}
