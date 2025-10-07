package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.faforever.commons.replay.body.ReplayBodyParser;
import com.faforever.commons.replay.body.ReplayBodyToken;
import com.faforever.commons.replay.body.ReplayBodyTokenizer;
import com.faforever.commons.replay.header.ReplayHeader;
import com.faforever.commons.replay.header.ReplayHeaderParser;
import com.faforever.commons.replay.header.Source;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class ReplayLoader {

  @Contract(pure = true)
  private static ReplayHeader loadSCFAReplayHeader(ByteBuffer buffer) {
    return ReplayHeaderParser.parse(buffer);
  }

  @Contract(pure = true)
  private static @NotNull List<RegisteredEvent> loadSCFAReplayBody(List<Source> sources, ByteBuffer buffer) {
    var rewindPosition = buffer.position();
    List<ReplayBodyToken> bodyTokens = ReplayBodyTokenizer.tokenize(buffer);
    buffer.position(rewindPosition);

    List<Event> bodyEvents = ReplayBodyParser.parseTokens(bodyTokens, buffer);
    return ReplaySemantics.registerEvents(sources, bodyEvents);
  }

  @Contract(pure = true)
  private static ReplayContainer loadSCFAReplayFromMemory(ReplayMetadata metadata, ByteBuffer scfaReplayBuffer) throws IOException {
    scfaReplayBuffer.order(ByteOrder.LITTLE_ENDIAN);

    ReplayHeader replayHeader = loadSCFAReplayHeader(scfaReplayBuffer);
    List<RegisteredEvent> replayBody = loadSCFAReplayBody(replayHeader.sources(), scfaReplayBuffer);

    if (scfaReplayBuffer.position() != scfaReplayBuffer.limit()) {
      throw new EOFException();
    }

    return new ReplayContainer(metadata, replayHeader, replayBody);
  }

  public static ReplayContainer loadSCFAReplayFromDisk(Path scfaReplayFile) throws IOException, IllegalArgumentException {
    if (!scfaReplayFile.toString().toLowerCase().endsWith("scfareplay")) {
      throw new IllegalArgumentException("Unknown file format: " + scfaReplayFile.getFileName());
    }

    byte[] bytes = Files.readAllBytes(scfaReplayFile);
    return loadSCFAReplayFromMemory(null, ByteBuffer.wrap(bytes));
  }

  @Contract(pure = true)
  private static ReplayContainer loadFAFReplayFromMemory(byte[] fafReplayBytes) throws IOException, CompressorException {
    int separator = findSeparatorIndex(fafReplayBytes);
    ByteBuffer buffer = ByteBuffer.wrap(fafReplayBytes);

    buffer.limit(separator);
    final String decodedMetadata = StandardCharsets.UTF_8.newDecoder().decode(buffer).toString();

    ObjectMapper parsedMetadata = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ReplayMetadata replayMetadata = parsedMetadata.readValue(decodedMetadata, ReplayMetadata.class);
    buffer.limit(buffer.capacity());

    buffer.position(separator + 1);
    ByteBuffer scfaReplayBuffer= decompress(buffer, replayMetadata);

    return loadSCFAReplayFromMemory(replayMetadata, scfaReplayBuffer);
  }

  public static ReplayContainer loadFAFReplayFromDisk(Path fafReplayFile) throws IOException, CompressorException, IllegalArgumentException {
    if (!fafReplayFile.toString().toLowerCase().endsWith("fafreplay")) {
      throw new IllegalArgumentException("Unknown file format: " + fafReplayFile.getFileName());
    }

    byte[] fafReplayBytes = Files.readAllBytes(fafReplayFile);
    return loadFAFReplayFromMemory(fafReplayBytes);
  }

  private static int findSeparatorIndex(byte[] replayData) {
    for (int headerEnd = 0; headerEnd < replayData.length; headerEnd++) {
      if (replayData[headerEnd] == '\n') {
        return headerEnd;
      }
    }
    throw new IllegalArgumentException("Missing separator between replay header and body");
  }

  private static ByteBuffer decompress(ByteBuffer inputBuffer, @NotNull ReplayMetadata metadata) throws IOException, CompressorException {
    CompressionType compressionType = Objects.requireNonNullElse(metadata.getCompression(), CompressionType.QTCOMPRESS);

    switch (compressionType) {
      case QTCOMPRESS: {
        return QtCompress.qUncompress(Base64.getDecoder().decode(inputBuffer));
      }
      case ZSTD: {
        byte[] inputArray = new byte[inputBuffer.remaining()];
        inputBuffer.get(inputArray);
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(inputArray);
        CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(arrayInputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, out);
        return ByteBuffer.wrap(out.toByteArray());
      }
      case UNKNOWN:
      default:
        throw new IOException("Unknown replay format in replay file");
    }
  }
}
