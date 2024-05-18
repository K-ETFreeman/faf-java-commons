package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.faforever.commons.replay.shared.LuaData;
import com.faforever.commons.replay.body.ReplayBodyParser;
import com.faforever.commons.replay.body.ReplayBodyToken;
import com.faforever.commons.replay.body.ReplayBodyTokenizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.google.common.io.LittleEndianDataInputStream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@SuppressWarnings("unused")
@Slf4j
public class ReplayDataParser {

  private static final int LUA_NUMBER = 0;
  private static final int LUA_STRING = 1;
  private static final int LUA_NIL = 2;
  private static final int LUA_BOOL = 3;
  private static final int LUA_TABLE_START = 4;
  private static final int LUA_TABLE_END = 5;
  private final Path path;
  private final ObjectMapper objectMapper;
  @Getter
  private ReplayMetadata metadata;
  @Getter
  private String replayPatchFieldId;
  @Getter
  private byte[] data;
  @Getter
  private String map;
  @Getter
  private Map<String, Map<String, ?>> mods;
  @Getter
  private final Map<Integer, Map<String, Object>> armies = new HashMap<>();
  private int randomSeed;
  @Getter
  private final List<ChatMessage> chatMessages = new ArrayList<>();
  @Getter
  private final List<ModeratorEvent> moderatorEvents = new ArrayList<>();
  @Getter
  private final Map<Integer, Map<Integer, AtomicInteger>> commandsPerMinuteByPlayer = new HashMap<>();

  private int ticks;

  @Getter
  private List<GameOption> gameOptions;

  @Getter
  private List<ReplayBodyToken> tokens;

  @Getter
  private List<Event> events;

  public ReplayDataParser(Path path, ObjectMapper objectMapper) throws IOException, CompressorException {
    this.path = path;
    this.objectMapper = objectMapper;

    parse();
  }

  @VisibleForTesting
  static String readString(LittleEndianDataInputStream dataStream) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte tempByte;
    while ((tempByte = dataStream.readByte()) != 0) {
      out.write(tempByte);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  private Object parseLua(LittleEndianDataInputStream dataStream) throws IOException {
    int type = dataStream.readUnsignedByte();
    switch (type) {
      case LUA_NUMBER:
        return dataStream.readFloat();
      case LUA_STRING:
        return readString(dataStream);
      case LUA_NIL:
        dataStream.skipBytes(1);
        return null;
      case LUA_BOOL: // bool
        return dataStream.readUnsignedByte() == 0;
      case LUA_TABLE_START: // lua
        Map<String, Object> result = new HashMap<>();
        while (peek(dataStream) != LUA_TABLE_END) {
          Object key = parseLua(dataStream);
          if (key instanceof Number) {
            key = ((Number) key).intValue();
          }
          result.put(String.valueOf(key), parseLua(dataStream));
          dataStream.mark(1);
        }
        dataStream.skipBytes(1);
        return result;
      default:
        throw new IllegalStateException("Unexpected data type: " + type);
    }
  }

  private int peek(LittleEndianDataInputStream dataStream) throws IOException {
    dataStream.mark(1);
    int next = dataStream.readUnsignedByte();
    dataStream.reset();
    return next;
  }

  private void readReplayData(Path replayFile) throws IOException, CompressorException {
    byte[] allReplayData = Files.readAllBytes(replayFile);
    int headerEnd = findReplayHeaderEnd(allReplayData);
    metadata = objectMapper.readValue(new String(Arrays.copyOf(allReplayData, headerEnd), StandardCharsets.UTF_8), ReplayMetadata.class);
    data = decompress(Arrays.copyOfRange(allReplayData, headerEnd + 1, allReplayData.length), metadata);
  }

  private int findReplayHeaderEnd(byte[] replayData) {
    int headerEnd;
    for (headerEnd = 0; headerEnd < replayData.length; headerEnd++) {
      if (replayData[headerEnd] == '\n') {
        return headerEnd;
      }
    }
    throw new IllegalArgumentException("Missing separator between replay header and body");
  }

  private byte[] decompress(byte[] data, @NotNull ReplayMetadata metadata) throws IOException, CompressorException {
    CompressionType compressionType = Objects.requireNonNullElse(metadata.getCompression(), CompressionType.QTCOMPRESS);

    switch (compressionType) {
      case QTCOMPRESS: {
        return QtCompress.qUncompress(BaseEncoding.base64().decode(new String(data)));
      }
      case ZSTD: {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        CompressorInputStream compressorInputStream = new CompressorStreamFactory().createCompressorInputStream(arrayInputStream);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(compressorInputStream, out);
        return out.toByteArray();
      }
      case UNKNOWN:
      default:
        throw new IOException("Unknown replay format in replay file");
    }
  }

  @SuppressWarnings("unchecked")
  private void parseHeader(LittleEndianDataInputStream dataStream) throws IOException {
    replayPatchFieldId = readString(dataStream);
    String arg13 = readString(dataStream); // always \r\n

    String[] split = readString(dataStream).split("\\r\\n");
    String replayVersionId = split[0];
    map = split[1];
    String arg23 = readString((dataStream)); // always \r\n and some unknown character

    int sizeModsInBytes = dataStream.readInt();
    mods = (Map<String, Map<String, ?>>) parseLua(dataStream);

    int sizeGameOptionsInBytes = dataStream.readInt();
    this.gameOptions = ((Map<String, Object>) parseLua(dataStream)).entrySet().stream()
      .filter(entry -> "Options".equals(entry.getKey()))
      .flatMap(entry -> ((Map<String, Object>) entry.getValue()).entrySet().stream())
      .map(entry -> new GameOption(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList());

    int numberOfSources = dataStream.readUnsignedByte();

    Map<String, Object> playerIdsByName = new HashMap<>();
    for (int i = 0; i < numberOfSources; i++) {
      String playerName = readString(dataStream);
      int playerId = dataStream.readInt();
      playerIdsByName.put(playerName, playerId);
    }

    boolean cheatsEnabled = dataStream.readUnsignedByte() > 0;

    int numberOfArmies = dataStream.readUnsignedByte();
    for (int i = 0; i < numberOfArmies; i++) {
      int sizePlayerDataInBytes = dataStream.readInt();
      Map<String, Object> playerData = (Map<String, Object>) parseLua(dataStream);
      int playerSource = dataStream.readUnsignedByte();

      armies.put(playerSource, playerData);
      playerData.put("commands", new ArrayList<>());

      if (playerSource != 255) {
        dataStream.skipBytes(1);
      }
    }

    randomSeed = dataStream.readInt();
  }

  private void interpretEvents(List<Event> events) {
    Integer player = -1;
    boolean desync = false;
    String previousChecksum = null;
    int previousTick = -1;

    Map<Integer, Integer> lastTicks = new HashMap<>();

    for (Event event : events) {

      switch (event) {
        case Event.Unprocessed(ReplayBodyToken token, String reason) -> {

        }

        case Event.ProcessingError(ReplayBodyToken token, Exception exception) -> {

        }

        case Event.Advance(int ticksToAdvance) -> {
          ticks += ticksToAdvance;
        }

        case Event.SetCommandSource(int playerIndex) -> {
          player = playerIndex;
        }

        case Event.CommandSourceTerminated() -> {
          lastTicks.put(player, ticks);
        }

        case Event.VerifyChecksum(String hash, int tick) -> {
          desync = tick == previousTick && !Objects.equals(previousChecksum, hash);
          previousChecksum = hash;
          previousTick = ticks;

          if (desync) {
            log.warn("Replay desynced");
            return;
          }
        }

        case Event.RequestPause() -> {

        }

        case Event.RequestResume() -> {

        }

        case Event.SingleStep() -> {

        }

        case Event.CreateUnit(int playerIndex, String blueprintId, float px, float pz, float heading) -> {

        }

        case Event.CreateProp(String blueprintId, float px, float pz, float heading) -> {

        }

        case Event.DestroyEntity(int entityId) -> {

        }

        case Event.WarpEntity(int entityId, float px, float py, float pz) -> {

        }

        case Event.ProcessInfoPair(int entityId, String arg1, String arg2) -> {

        }

        case Event.IssueCommand(
          Event.CommandUnits commandUnits, Event.CommandData commandData
        ) -> {
          commandsPerMinuteByPlayer
            .computeIfAbsent(player, p -> new HashMap<>())
            .computeIfAbsent(ticks, t -> new AtomicInteger())
            .incrementAndGet();
        }

        case Event.IssueFactoryCommand(
          Event.CommandUnits commandUnits, Event.CommandData commandData
        ) -> {
          commandsPerMinuteByPlayer
            .computeIfAbsent(player, p -> new HashMap<>())
            .computeIfAbsent(ticks, t -> new AtomicInteger())
            .incrementAndGet();
        }

        case Event.IncreaseCommandCount(int commandId, int delta) -> {

        }

        case Event.DecreaseCommandCount(int commandId, int delta) -> {

        }

        case Event.SetCommandTarget(int commandId, Event.CommandTarget commandTarget) -> {

        }

        case Event.SetCommandType(int commandId, int targetId) -> {

        }

        case Event.SetCommandCells(int commandId, Object parametersLua, float px, float py, float pz) -> {

        }

        case Event.RemoveCommandFromQueue(int commandId, int unitId) -> {

        }

        case Event.DebugCommand(
          String command, float px, float py, float pz, byte focusArmy, Event.CommandUnits units
        ) -> {

        }

        case Event.ExecuteLuaInSim(String luaCode) -> {

        }

        case Event.LuaSimCallback(
          String func, LuaData.Table parametersLua, Event.CommandUnits commandUnits
        ) when func.equals("GiveResourcesToPlayer") -> {
          parseGiveResourcesToPlayer(parametersLua);
        }

        case Event.LuaSimCallback(
          String func, LuaData.Table parametersLua, Event.CommandUnits commandUnits
        ) when func.equals("ModeratorEvent") -> {
          parseModeratorEvent(parametersLua, player);
        }

        case Event.LuaSimCallback(
          String func, LuaData parametersLua, Event.CommandUnits commandUnits
        ) -> {

        }

        case Event.EndGame() -> {

        }

      }
    }
  }

  private void parseGiveResourcesToPlayer(LuaData.Table lua) {
    if (lua.value().containsKey("Msg") && lua.value().containsKey("From") && lua.value().containsKey("Sender")) {

      // TODO: use the command source (player value) instead of the values from the callback. The values from the callback can be manipulated
      if (!(lua.value().get("From") instanceof LuaData.Number(float luaFromArmy))) {
        return;
      }

      int fromArmy = (int) luaFromArmy - 1;
      if (fromArmy == -2) {
        return;
      }

      if (!(lua.value().get("Msg") instanceof LuaData.Table(Map<String, LuaData> luaMsg))) {
        return;
      }

      if (!(lua.value().get("Sender") instanceof LuaData.String(String luaSender))) {
        return;
      }

      // This can either be a player name or a Map of something, in which case it's actually giving resources
      if (!(luaMsg.get("to") instanceof LuaData.String(String luaMsgReceiver))) {
        return;
      }

      if (!(luaMsg.get("text") instanceof LuaData.String(String luaMsgText))) {
        return;
      }

      Map<String, Object> army = armies.get(fromArmy);
      if (army != null && Objects.equals(army.get("PlayerName"), luaSender)) {
        chatMessages.add(new ChatMessage(tickToTime(ticks), luaSender, String.valueOf(luaMsgReceiver), luaMsgText));
      }
    }
  }


  void parseModeratorEvent(LuaData.Table lua, Integer player) {
    String messageContent = null;
    String playerNameFromArmy = null;
    String playerNameFromCommandSource = null;
    Integer activeCommandSource = null;
    Integer fromArmy = null;

    if (lua.value().get("Message") instanceof LuaData.String(String luaMessage)) {
      messageContent = luaMessage;
    }

    if (lua.value().get("From") instanceof LuaData.Number(float luaFrom)) {
      fromArmy = (int) luaFrom - 1;


      if (fromArmy != -2) {
        Map<String, Object> army = armies.get(fromArmy);

        if (army != null) {
          playerNameFromArmy = (String) army.get("PlayerName");
        }
      }
    }

    if (player != null) {
      activeCommandSource = player;
      Map<String, Object> army = armies.get(activeCommandSource);

      if (army != null) {
        playerNameFromCommandSource = (String) army.get("PlayerName");
      }
    }

    moderatorEvents.add(new ModeratorEvent(tickToTime(ticks), activeCommandSource, fromArmy, messageContent, playerNameFromArmy, playerNameFromCommandSource));
  }

  private Duration tickToTime(int tick) {
    return Duration.ofSeconds(tick / 10);
  }

  private void parse() throws IOException, CompressorException {
    readReplayData(path);
    try (LittleEndianDataInputStream dataStream = new LittleEndianDataInputStream(new ByteArrayInputStream(data))) {
      parseHeader(dataStream);
      tokens = ReplayBodyTokenizer.tokenize(dataStream);
    }
    events = ReplayBodyParser.parseTokens(tokens);
    interpretEvents(events);
  }
}
