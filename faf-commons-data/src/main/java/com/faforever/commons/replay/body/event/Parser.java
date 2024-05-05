package com.faforever.commons.replay.body.event;

import com.faforever.commons.replay.body.token.Token;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Parser {
  public static List<Event> parseTokens(List<Token> tokens) throws IOException {
    return tokens.stream().parallel().map((token) -> {
      try {
        return parseToken(token);
      } catch (Exception exception) {
        return new Event.ProcessingError(token, exception);
      }
    }).toList();
  }

  private static int peek(LittleEndianDataInputStream dataStream) throws IOException {
    dataStream.mark(1);
    int next = dataStream.readUnsignedByte();
    dataStream.reset();
    return next;
  }

  private static String parseString(LittleEndianDataInputStream dataStream) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte tempByte;
    while ((tempByte = dataStream.readByte()) != 0) {
      out.write(tempByte);
    }
    return out.toString(StandardCharsets.UTF_8);
  }

  private static Event.CommandUnits parseCommandUnits(LittleEndianDataInputStream stream) throws IOException {
    int unitCount = stream.readInt();
    ArrayList<Integer> unitIds = new ArrayList<>(unitCount);
    for (int k = 0; k < unitCount; k++) {
      unitIds.add(stream.readInt());
    }

    return new Event.CommandUnits(unitCount, unitIds);
  }

  private static Event.CommandFormation parseCommandFormation(LittleEndianDataInputStream stream) throws IOException {
    float orientation = 0;
    float px = 0;
    float py = 0;
    float pz = 0;
    float scale = 0;

    int formation = stream.readInt();
    if (formation != -1) {
      orientation = stream.readFloat();
      px = stream.readFloat();
      py = stream.readFloat();
      pz = stream.readFloat();
      scale = stream.readFloat();
    }

    return new Event.CommandFormation(formation, orientation, px, py, pz, scale);
  }

  private static Event.CommandTarget parseCommandTarget(LittleEndianDataInputStream stream) throws IOException {
    CommandTargetType target = CommandTargetType.values()[stream.readByte()];
    switch (target) {
      case ENTITY -> {
        int entityId = stream.readInt();
        return new Event.CommandTarget.Entity(entityId);
      }

      case POSITION -> {
        float px = stream.readFloat();
        float py = stream.readFloat();
        float pz = stream.readFloat();
        return new Event.CommandTarget.Position(px, py, pz);
      }

      default -> {
        return null;
      }
    }
  }

  private static Event.CommandData parseCommandData(LittleEndianDataInputStream stream) throws IOException {
    int commandId = stream.readInt();
    byte[] arg1 = stream.readNBytes(4);
    EventCommandType commandType = EventCommandType.values()[stream.readByte()];
    byte[] arg2 = stream.readNBytes(4);

    Event.CommandTarget commandTarget = parseCommandTarget(stream);

    byte[] arg3 = stream.readNBytes(1);

    Event.CommandFormation commandFormation = parseCommandFormation(stream);

    String blueprintId = parseString(stream);
    byte[] arg4 = stream.readNBytes(12);
    byte[] arg5 = new byte[0];

    LuaData parametersLua = parseLua(stream);
    if (!(parametersLua instanceof LuaData.Nil)) {
      arg5 = stream.readNBytes(1);
    }

    return new Event.CommandData(
      commandId, commandType, commandTarget, commandFormation, blueprintId, parametersLua
    );
  }

  private static LuaData parseLua(LittleEndianDataInputStream dataStream) throws IOException {
    int type = dataStream.readUnsignedByte();

    final int LUA_NUMBER = 0;
    final int LUA_STRING = 1;
    final int LUA_NIL = 2;
    final int LUA_BOOL = 3;
    final int LUA_TABLE_START = 4;
    final int LUA_TABLE_END = 5;

    switch (type) {
      case LUA_NUMBER -> {
        float value = dataStream.readFloat();
        return new LuaData.Number(value);
      }

      case LUA_STRING -> {
        String value = parseString(dataStream);
        return new LuaData.String(value);
      }


      case LUA_NIL -> {
        dataStream.skipBytes(1);
        return new LuaData.Nil();
      }

      case LUA_BOOL -> {
        boolean value = dataStream.readUnsignedByte() == 0;
        return new LuaData.Bool(value);
      }

      case LUA_TABLE_START -> {
        Map<String, LuaData> value = new HashMap<>();
        while (peek(dataStream) != LUA_TABLE_END) {
          LuaData key = parseLua(dataStream);

          switch (key) {
            case LuaData.String(String str) -> value.put(str, parseLua(dataStream));

            case LuaData.Number(float num) -> value.put(String.valueOf(num), parseLua(dataStream));

            default -> throw new IllegalStateException("Unexpected data type: " + type);
          }

          dataStream.mark(1);
        }
        dataStream.skipBytes(1);

        return new LuaData.Table(value);
      }
      default -> throw new IllegalStateException("Unexpected data type: " + type);
    }
  }

  private static Event parseToken(Token token) throws IOException {

    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(token.tokenContent())))) {
      return switch (token.tokenId()) {
        case CMDST_ADVANCE -> {
          int ticks = stream.readInt();
          yield new Event.Advance(ticks);
        }

        case CMDST_SET_COMMAND_SOURCE -> {
          int playerIndex = stream.readByte();
          yield new Event.SetCommandSource(playerIndex);
        }

        case CMDST_COMMAND_SOURCE_TERMINATED -> new Event.CommandSourceTerminated();

        case CMDST_VERIFY_CHECKSUM -> {
          String hash = HexFormat.of().formatHex(stream.readNBytes(16));
          int tick = stream.readInt();

          yield new Event.VerifyChecksum(hash, tick);
        }

        case CMDST_REQUEST_PAUSE -> new Event.RequestPause();

        case CMDST_RESUME -> new Event.RequestResume();

        case CMDST_SINGLE_STEP -> new Event.SingleStep();

        case CMDST_CREATE_UNIT -> {
          int playerIndex = stream.readByte();
          String blueprintId = parseString(stream);
          float px = stream.readFloat();
          float pz = stream.readFloat();
          float heading = stream.readFloat();

          yield new Event.CreateUnit(playerIndex, blueprintId, px, pz, heading);
        }

        case CMDST_CREATE_PROP -> {
          String blueprintId = parseString(stream);
          float px = stream.readFloat();
          float pz = stream.readFloat();
          float heading = stream.readFloat();

          yield new Event.CreateProp(blueprintId, px, pz, heading);
        }

        case CMDST_DESTROY_ENTITY -> {
          int entityId = stream.readInt();
          yield new Event.DestroyEntity(entityId);
        }

        case CMDST_WARP_ENTITY -> {
          int entityId = stream.readInt();
          float px = stream.readFloat();
          float py = stream.readFloat();
          float pz = stream.readFloat();
          yield new Event.WarpEntity(entityId, px, py, pz);
        }

        case CMDST_PROCESS_INFO_PAIR -> {
          int entityId = stream.read();
          String arg1 = parseString(stream);
          String arg2 = parseString(stream);
          yield new Event.ProcessInfoPair(entityId, arg1, arg2);
        }

        case CMDST_ISSUE_COMMAND -> {
          Event.CommandUnits commandUnits = parseCommandUnits(stream);
          Event.CommandData commandData = parseCommandData(stream);

          yield new Event.IssueCommand(commandUnits, commandData);
        }

        case CMDST_ISSUE_FACTORY_COMMAND -> {
          Event.CommandUnits commandUnits = parseCommandUnits(stream);
          Event.CommandData commandData = parseCommandData(stream);

          yield new Event.IssueFactoryCommand(commandUnits, commandData);
        }

        case CMDST_INCREASE_COMMAND_COUNT -> {
          int commandId = stream.readInt();
          int delta = stream.readInt();
          yield new Event.IncreaseCommandCount(commandId, delta);
        }

        case CMDST_DECRASE_COMMAND_COUNT -> {
          int commandId = stream.readInt();
          int delta = stream.readInt();
          yield new Event.DecreaseCommandCount(commandId, delta);
        }

        case CMDST_SET_COMMAND_TARGET -> {
          int commandId = stream.readInt();
          Event.CommandTarget commandTarget = parseCommandTarget(stream);
          yield new Event.SetCommandTarget(commandId, commandTarget);
        }

        case CMDST_SET_COMMAND_TYPE -> {
          int commandId = stream.readInt();
          int targetCommandType = stream.readInt();
          yield new Event.SetCommandType(commandId, targetCommandType);
        }

        case CMDST_SET_COMMAND_CELLS -> {
          int commandId = stream.readInt();
          LuaData parametersLua = parseLua(stream);
          if (!(parametersLua instanceof LuaData.Nil)) {
            stream.readNBytes(1);
          }

          float px = stream.readFloat();
          float py = stream.readFloat();
          float pz = stream.readFloat();

          yield new Event.SetCommandCells(commandId, parametersLua, px, py, pz);
        }

        case CMDST_REMOVE_COMMAND_FROM_QUEUE -> {
          int commandId = stream.readInt();
          int unitId = stream.readInt();
          yield new Event.RemoveCommandFromQueue(commandId, unitId);
        }

        case CMDST_DEBUG_COMMAND -> new Event.Unprocessed(token, "CMDST_DEBUG_COMMAND");

        case CMDST_EXECUTE_LUA_IN_SIM -> {
          String luaCode = parseString(stream);
          yield new Event.ExecuteLuaInSim(luaCode);
        }

        case CMDST_LUA_SIM_CALLBACK -> {
          String func = parseString(stream);
          LuaData args = parseLua(stream);
          Event.CommandUnits commandUnits = null;

          // suspicion that this is just flat out wrong! Whether there's a selection in the data is not related to whether there are Lua arguments
          if (!(args instanceof LuaData.Nil)) {
            commandUnits = parseCommandUnits(stream);
          } else {
            // the '4' we read here is the size, I suspect the 3 bytes are maybe to align the data somehow? No idea
            stream.readNBytes(4 + 3);
          }

          yield new Event.LuaSimCallback(func, args, commandUnits);
        }

        case CMDST_END_GAME -> new Event.EndGame();

        case null -> new Event.Unprocessed(token, "Unknown");
      };
    }
  }

  private enum CommandTargetType {
    // Order is crucial
    NONE,
    ENTITY,
    POSITION
  }
}
