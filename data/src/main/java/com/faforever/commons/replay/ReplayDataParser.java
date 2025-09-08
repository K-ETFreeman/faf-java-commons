package com.faforever.commons.replay;

import com.faforever.commons.replay.body.Event;
import com.faforever.commons.replay.body.ReplayBodyParser;
import com.faforever.commons.replay.body.ReplayBodyToken;
import com.faforever.commons.replay.body.ReplayBodyTokenizer;
import com.faforever.commons.replay.shared.LoadUtils;
import com.faforever.commons.replay.shared.LuaData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
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
  private Map<String, Integer> playerIdsByName;

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
  static String readString(ByteBuffer buffer) {
    final int offset = buffer.position();
    while (buffer.get() != 0) {
    }
    final int length = buffer.position() - 1 - offset;
    byte[] stringBytes = new byte[length];
    buffer.get(offset, stringBytes);
    return new String(stringBytes, StandardCharsets.UTF_8);
  }

  private Object parseLua(ByteBuffer buffer) {
    int type = LoadUtils.getUnsignedByte(buffer);
    switch (type) {
      case LUA_NUMBER:
        return buffer.getFloat();
      case LUA_STRING:
        return readString(buffer);
      case LUA_NIL:
        buffer.get();
        return null;
      case LUA_BOOL: // bool
        return LoadUtils.getUnsignedByte(buffer) == 0;
      case LUA_TABLE_START: // lua
        Map<String, Object> result = new HashMap<>();
        while (peek(buffer) != LUA_TABLE_END) {
          Object key = parseLua(buffer);
          if (key instanceof Number) {
            key = ((Number) key).intValue();
          }
          result.put(String.valueOf(key), parseLua(buffer));
          buffer.mark();
        }
        buffer.get();
        return result;
      default:
        throw new IllegalStateException("Unexpected data type: " + type);
    }
  }

  private int peek(ByteBuffer buffer) {
    buffer.mark();
    int next = LoadUtils.getUnsignedByte(buffer);
    buffer.reset();
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
  private void parseHeader(ByteBuffer buffer) {
    replayPatchFieldId = readString(buffer);
    String arg13 = readString(buffer); // always \r\n

    String[] split = readString(buffer).split("\\r\\n");
    String replayVersionId = split[0];
    map = split[1];
    String arg23 = readString((buffer)); // always \r\n and some unknown character

    int sizeModsInBytes = buffer.getInt();
    mods = (Map<String, Map<String, ?>>) parseLua(buffer);

    int sizeGameOptionsInBytes = buffer.getInt();
    this.gameOptions = ((Map<String, Object>) parseLua(buffer)).entrySet().stream()
      .filter(entry -> "Options".equals(entry.getKey()))
      .flatMap(entry -> ((Map<String, Object>) entry.getValue()).entrySet().stream())
      .map(entry -> new GameOption(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList());

    int numberOfSources = LoadUtils.getUnsignedByte(buffer);

    playerIdsByName = new HashMap<>();
    for (int i = 0; i < numberOfSources; i++) {
      String playerName = readString(buffer);
      int playerId = buffer.getInt();
      playerIdsByName.put(playerName, playerId);
    }

    boolean cheatsEnabled = LoadUtils.getUnsignedByte(buffer) > 0;

    int numberOfArmies = LoadUtils.getUnsignedByte(buffer);
    for (int i = 0; i < numberOfArmies; i++) {
      int sizePlayerDataInBytes = buffer.getInt();
      Map<String, Object> playerData = (Map<String, Object>) parseLua(buffer);
      int playerSource = LoadUtils.getUnsignedByte(buffer);

      armies.put(playerSource, playerData);
      playerData.put("commands", new ArrayList<>());

      if (playerSource != 255) {
        buffer.get();
      }
    }

    randomSeed = buffer.getInt();
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

        }

        case Event.IssueFactoryCommand(
          Event.CommandUnits commandUnits, Event.CommandData commandData
        ) -> {

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
    LuaData msg;
    LuaData from;
    LuaData sender;
    if ((msg = lua.value().get("Msg")) != null && (from = lua.value().get("From")) != null && (sender = lua.value().get("Sender")) != null) {

      // TODO: use the command source (player value) instead of the values from the callback. The values from the callback can be manipulated
      if (!(from instanceof LuaData.Number(float luaFromArmy))) {
        return;
      }

      int fromArmy = (int) luaFromArmy - 1;
      if (fromArmy == -2) {
        return;
      }

      if (!(msg instanceof LuaData.Table(Map<String, LuaData> luaMsg))) {
        return;
      }

      if (!(sender instanceof LuaData.String(String luaSender))) {
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

      final ByteBuffer buffer = ByteBuffer.wrap(data);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      parseHeader(buffer);

      var rewindPosition = buffer.position();
      tokens = ReplayBodyTokenizer.tokenize(buffer);
      buffer.position(rewindPosition);

      events = ReplayBodyParser.parseTokens(tokens, buffer);
      interpretEvents(events);
  }
}
