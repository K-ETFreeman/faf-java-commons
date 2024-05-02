package com.faforever.commons.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.BaseEncoding;
import com.google.common.io.LittleEndianDataInputStream;
import lombok.Data;
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
  private Map<Integer, Map<String, Object>> armies;
  private int randomSeed;
  @Getter
  private List<ChatMessage> chatMessages;
  @Getter
  private List<ModeratorEvent> moderatorEvents;
  @Getter
  private Map<Integer, Map<Integer, AtomicInteger>> commandsPerMinuteByPlayer;
  private float x;
  private float y;
  private float z;
  private float w;
  private float scale;
  private int ticks;
  @Getter
  private List<GameOption> gameOptions;

  public ReplayDataParser(Path path, ObjectMapper objectMapper) throws IOException, CompressorException {
    this.path = path;
    this.objectMapper = objectMapper;
    armies = new HashMap<>();
    chatMessages = new ArrayList<>();
    moderatorEvents = new ArrayList<>();
    commandsPerMinuteByPlayer = new HashMap<>();
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

  @SuppressWarnings("unchecked")
  private void parseHeader(LittleEndianDataInputStream dataStream) throws IOException {
    replayPatchFieldId = readString(dataStream);
    dataStream.skipBytes(3);

    String[] split = readString(dataStream).split("\\r\\n");
    String replayVersionId = split[0];
    map = split[1];
    dataStream.skipBytes(4);

    int numberOfMods = dataStream.readInt();
    mods = (Map<String, Map<String, ?>>) parseLua(dataStream);

    int scenarioSize = dataStream.readInt();
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
      dataStream.skipBytes(4);
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

  @SuppressWarnings("unchecked")
  private void parseTicks(LittleEndianDataInputStream dataStream) throws IOException {
    Integer player = -1;
    boolean desync = false;
    byte[] previousChecksum = null;
    int previousTick = -1;

    Map<Integer, Integer> lastTicks = new HashMap<>();

    int commandId;
    while (dataStream.available() > 0) {
      commandId = dataStream.readUnsignedByte();
      int messageLength = dataStream.readUnsignedShort();
      try {
        Command command = Command.values()[commandId];
        switch (command) {
          case CMDST_ADVANCE:
            ticks += dataStream.readInt();
            break;

          case CMDST_SET_COMMAND_SOURCE:
            player = dataStream.readUnsignedByte();
            break;

          case CMDST_COMMAND_SOURCE_TERMINATED:
            lastTicks.put(player, ticks);
            break;

          case CMDST_VERIFY_CHECKSUM:
            if (desync) {
              break;
            }
            byte[] checksum = readCheckSum(dataStream);
            int tickNum = dataStream.readInt();

            desync = tickNum == previousTick && !Arrays.equals(previousChecksum, checksum);

            previousChecksum = checksum;
            previousTick = ticks;
            break;

          case CMDST_SET_COMMAND_TARGET:
            dataStream.skipBytes(4);
            StiTarget stiTarget = StiTarget.values()[dataStream.readUnsignedByte()];
            switch (stiTarget) {
              case ENTITY:
                int entityId = dataStream.readInt();
                break;
              case POSITION:
                float x = dataStream.readFloat();
                float y = dataStream.readFloat();
                float z = dataStream.readFloat();
                break;
            }
            break;

          case CMDST_PROCESS_INFO_PAIR:
            dataStream.readInt();
            readString(dataStream);
            readString(dataStream);
            break;

          case CMDST_LUA_SIM_CALLBACK:
            String functionName = readString(dataStream);
            Object lua = parseLua(dataStream);

            if (Objects.equals("GiveResourcesToPlayer", functionName)) {
              parseGiveResourcesToPlayer((Map<String, Object>) lua);
            }

            if (Objects.equals("ModeratorEvent", functionName)) {
              parseModeratorEvent((Map<String, Object>) lua, player);
            }

            // No idea what this skips
            if (lua != null) {
              dataStream.skipBytes(4 * dataStream.readInt());
            } else {
              dataStream.skipBytes(4 + 3);
            }
            break;

          case CMDST_ISSUE_COMMAND:
          case CMDST_ISSUE_FACTORY_COMMAND:
            commandsPerMinuteByPlayer
              .computeIfAbsent(player, p -> new HashMap<>())
              .computeIfAbsent(ticks, t -> new AtomicInteger())
              .incrementAndGet();

            int unitNums = dataStream.readInt();
            dataStream.skipBytes(unitNums * 4);

            dataStream.skipBytes(8);
            int commandType = dataStream.readUnsignedByte();
            dataStream.skipBytes(4);

            switch (StiTarget.values()[dataStream.readUnsignedByte()]) {
              case NONE:
                break;
              case ENTITY:
                int entityId = dataStream.readInt();
                break;
              case POSITION:
                x = dataStream.readFloat();
                y = dataStream.readFloat();
                z = dataStream.readFloat();
                break;
              default:
            }

            dataStream.skipBytes(1);
            int formation = dataStream.readInt();
            if (formation != -1) {
              w = dataStream.readFloat();
              x = dataStream.readFloat();
              y = dataStream.readFloat();
              z = dataStream.readFloat();
              scale = dataStream.readFloat();
            }

            String bp = readString(dataStream);
            dataStream.skipBytes(4 + 4 + 4);
            Object upgradeLua = parseLua(dataStream);
            if (upgradeLua != null) {
              dataStream.skipBytes(1);
            }

            ((List<NotSure>) armies.get(player).get("commands")).add(new NotSure(ticks, commandType, bp, upgradeLua != null));
            break;

          case CMDST_RESUME:
          case CMDST_REQUEST_PAUSE:
          case CMDST_END_GAME:
            break;

          case CMDST_SET_COMMAND_TYPE:
            dataStream.skipBytes(8);
            break;

          default:
            dataStream.skipBytes(messageLength - 3);
        }
      } catch (Exception throwable) {
        log.warn("Unable to determine command {}", commandId, throwable);
        dataStream.skipBytes(messageLength - 3);
      }
    }
  }

  private void parseGiveResourcesToPlayer(Map<String, Object> lua) {
    if (lua.containsKey("Msg") && lua.containsKey("From") && lua.containsKey("Sender")) {
      int fromArmy = ((Number) lua.get("From")).intValue() - 1;
      if (fromArmy != -2) {
        Map<String, String> msg = (Map<String, String>) lua.get("Msg");
        String sender = (String) lua.get("Sender");
        // This can either be a player name or a Map of something, in which case it's actually giving resources
        Object receiver = msg.get("to");
        if (receiver instanceof String) {
          String text = msg.get("text");

          Map<String, Object> army = armies.get(fromArmy);
          if (army != null && Objects.equals(army.get("PlayerName"), sender)) {
            chatMessages.add(new ChatMessage(tickToTime(ticks), sender, String.valueOf(receiver), text));
          }
        }
      }
    }
  }


  void parseModeratorEvent(Map<String, Object> lua, Integer player) {
    String messageContent = null;
    String playerNameFromArmy = null;
    String playerNameFromCommandSource = null;
    Integer activeCommandSource = null;
    Integer fromArmy = null;

    if (lua.containsKey("Message") && lua.get("Message") instanceof String value) {
      messageContent = value;
    }

    if (lua.containsKey("From") && lua.get("From") instanceof Number value) {
      fromArmy = value.intValue() - 1;

      if (fromArmy != -2) {
        Map<String, Object> army = armies.get(fromArmy);

        if (army != null){
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

    moderatorEvents.add(new ModeratorEvent(tickToTime(ticks), activeCommandSource, fromArmy,
      messageContent, playerNameFromArmy, playerNameFromCommandSource));
  }

  private Duration tickToTime(int tick) {
    return Duration.ofSeconds(tick / 10);
  }

  private byte[] readCheckSum(LittleEndianDataInputStream dataStream) throws IOException {
    byte[] bytes = new byte[16];
    dataStream.read(bytes);
    return bytes;
  }

  private void parse() throws IOException, CompressorException {
    readReplayData(path);
    LittleEndianDataInputStream dataStream = new LittleEndianDataInputStream(new ByteArrayInputStream(data));
    parseHeader(dataStream);
    parseTicks(dataStream);
  }


  private enum StiTarget {
    NONE,
    ENTITY,
    POSITION
  }

  private enum Command {
    CMDST_ADVANCE,
    CMDST_SET_COMMAND_SOURCE,
    CMDST_COMMAND_SOURCE_TERMINATED,
    CMDST_VERIFY_CHECKSUM,
    CMDST_REQUEST_PAUSE,
    CMDST_RESUME,
    CMDST_SINGLE_STEP,
    CMDST_CREATE_UNIT,
    CMDST_CREATE_PROP,
    CMDST_DESTROY_ENTITY,
    CMDST_WARP_ENTITY,
    CMDST_PROCESS_INFO_PAIR,
    CMDST_ISSUE_COMMAND,
    CMDST_ISSUE_FACTORY_COMMAND,
    CMDST_INCREASE_COMMAND_COUNT,
    CMDST_DECRASE_COMMAND_COUNT,
    CMDST_SET_COMMAND_TARGET,
    CMDST_SET_COMMAND_TYPE,
    CMDST_SET_COMMAND_CELLS,
    CMDST_REMOVE_COMMAND_FROM_QUEUE,
    CMDST_DEBUG_COMMAND,
    CMDST_EXECUTE_LUA_IN_SIM,
    CMDST_LUA_SIM_CALLBACK,
    CMDST_END_GAME
  }

  public enum EUnitCommandType {
    NONE("NONE"),
    STOP("Stop"),
    MOVE("Move"),
    DIVE("Dive"),
    FORM_MOVE("FormMove"),
    BUILD_SILO_TACTICAL("BuildSiloTactical"),
    BUILD_SILO_NUKE("BuildSiloNuke"),
    BUILD_FACTORY("BuildFactory"),
    BUILD_MOBILE("BuildMobile"),
    BUILD_ASSIsT("BuildAssist"),
    ATTACK("Attack"),
    FORM_ATTACK("FormAttack"),
    NUKE("Nuke"),
    TACTICAL("Tactical"),
    TELEPORT("Teleport"),
    GUARD("Guard"),
    PATROL("Patrol"),
    FERRY("Ferry"),
    FORM_PATROL("FormPatrol"),
    RECLAIM("Reclaim"),
    REPAIR("Repair"),
    CAPTURE("Capture"),
    TRANSPORT_LOAD_UNITS("TransportLoadUnits"),
    TRANSPORT_REVERSE_LOAD_UNITS("TransportReverseLoadUnits"),
    TRANSPORT_UNLOAD_UNITS("TransportUnloadUnits"),
    TRANSPORT_UNLOAD_SPECIFIC_UNITS("TransportUnloadSpecificUnits"),
    DETACH_FROM_TRANSPORT("DetachFromTransport"),
    UPGRADE("Upgrade"),
    SCRIPT("Script"),
    ASSIST_COMMANDER("AssistCommander"),
    KILL_SELF("KillSelf"),
    DESTROY_SELF("DestroySelf"),
    SACRIFICE("Sacrifice"),
    PAUSE("Pause"),
    OVER_CHARGE("OverCharge"),
    AGGRESSIVE_MOVE("AggressiveMove"),
    FORM_AGGRESSIVE_MOVE("FormAggressiveMove"),
    ASSIST_MOVE("AssistMove"),
    SPECIAL_ACTION("SpecialAction"),
    DOCK("Dock");

    private final String string;

    EUnitCommandType(String string) {
      this.string = string;
    }
  }

  @Data
  private class NotSure {

    private final int tick;
    private final int commandType;
    private final String bp;
    private final boolean upgradeLua;
  }
}
