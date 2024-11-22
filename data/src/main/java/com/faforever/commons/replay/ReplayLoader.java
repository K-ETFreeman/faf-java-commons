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
import com.google.common.io.BaseEncoding;
import com.google.common.io.LittleEndianDataInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ReplayLoader {

  @Contract(pure = true)
  private static ReplayHeader loadSCFAReplayHeader(LittleEndianDataInputStream stream) throws IOException {
    return ReplayHeaderParser.parse(stream);
  }

  @Contract(pure = true)
  private static @NotNull List<RegisteredEvent> loadSCFAReplayBody(List<Source> sources, LittleEndianDataInputStream stream) throws IOException {
    List<ReplayBodyToken> bodyTokens = ReplayBodyTokenizer.tokenize(stream);
    List<Event> bodyEvents = ReplayBodyParser.parseTokens(bodyTokens);
    return ReplaySemantics.registerEvents(sources, bodyEvents);
  }

  @Contract(pure = true)
  private static ReplayContainer loadSCFAReplayFromMemory(ReplayMetadata metadata, byte[] scfaReplayBytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(scfaReplayBytes)))) {
      ReplayHeader replayHeader = loadSCFAReplayHeader(stream);
      List<RegisteredEvent> replayBody = loadSCFAReplayBody(replayHeader.sources(), stream);

      if (stream.available() > 0) {
        throw new EOFException();
      }

      return new ReplayContainer(metadata, replayHeader, replayBody);
    }
  }

  public static ReplayContainer loadSCFAReplayFromDisk(Path scfaReplayFile) throws IOException, IllegalArgumentException {
    if (!scfaReplayFile.toString().toLowerCase().endsWith("scfareplay")) {
      throw new IllegalArgumentException("Unknown file format: " + scfaReplayFile.getFileName());
    }

    byte[] bytes = Files.readAllBytes(scfaReplayFile);
    return loadSCFAReplayFromMemory(null, bytes);
  }

  @Contract(pure = true)
  private static ReplayContainer loadFAFReplayFromMemory(byte[] fafReplayBytes) throws IOException, CompressorException {
    int separator = findSeparatorIndex(fafReplayBytes);
    byte[] metadataBytes = Arrays.copyOfRange(fafReplayBytes, 0, separator);
    String metadataString = new String(metadataBytes, StandardCharsets.UTF_8);

    ObjectMapper parsedMetadata = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ReplayMetadata replayMetadata = parsedMetadata.readValue(metadataString, ReplayMetadata.class);

    byte[] compressedReplayBytes = Arrays.copyOfRange(fafReplayBytes, separator + 1, fafReplayBytes.length);
    byte[] scfaReplayBytes = decompress(compressedReplayBytes, replayMetadata);

    return loadSCFAReplayFromMemory(replayMetadata, scfaReplayBytes);
  }

  public static ReplayContainer loadFAFReplayFromDisk(Path fafReplayFile) throws IOException, CompressorException, IllegalArgumentException {
    if (!fafReplayFile.toString().toLowerCase().endsWith("fafreplay")) {
      throw new IllegalArgumentException("Unknown file format: " + fafReplayFile.getFileName());
    }

    byte[] fafReplayBytes = Files.readAllBytes(fafReplayFile);
    return loadFAFReplayFromMemory(fafReplayBytes);
  }

  private static int findSeparatorIndex(byte[] replayData) {
    int headerEnd;
    for (headerEnd = 0; headerEnd < replayData.length; headerEnd++) {
      if (replayData[headerEnd] == '\n') {
        return headerEnd;
      }
    }
    throw new IllegalArgumentException("Missing separator between replay header and body");
  }

  private static byte[] decompress(byte[] data, @NotNull ReplayMetadata metadata) throws IOException, CompressorException {
    CompressionType compressionType = Objects.requireNonNullElse(metadata.getCompression(), CompressionType.QTCOMPRESS);

    switch (compressionType) {
      case QTCOMPRESS: {
        return QtCompress.qUncompress(BaseEncoding.base64().decode(new String(data)));
      }
      case ZSTD: {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        CompressorInputStream compressorInputStream = new CompressorStreamFactory()
          .createCompressorInputStream(arrayInputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, out);
        return out.toByteArray();
      }
      case UNKNOWN:
      default:
        throw new IOException("Unknown replay format in replay file");
    }
  }
}
