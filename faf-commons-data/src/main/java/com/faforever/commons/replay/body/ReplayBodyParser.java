package com.faforever.commons.replay.body;

import com.faforever.commons.replay.shared.LuaData;
import com.faforever.commons.replay.shared.LoadUtils;
import com.google.common.io.LittleEndianDataInputStream;
import org.jetbrains.annotations.Contract;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.*;

public class ReplayBodyParser {

  @Contract(pure = true)
  public static List<Event> parseTokens(List<ReplayBodyToken> tokens) throws IOException {
    return tokens.stream().map((token) -> {
      try {
        return parseToken(token);
      } catch (Exception exception) {
        return new Event.ProcessingError(token, exception);
      }
    }).toList();
  }

  @Contract(pure = true)
  private static Event.CommandUnits parseCommandUnits(LittleEndianDataInputStream stream) throws IOException {
    int unitCount = stream.readInt();
    ArrayList<Integer> unitIds = new ArrayList<>(unitCount);
    for (int k = 0; k < unitCount; k++) {
      unitIds.add(stream.readInt());
    }

    return new Event.CommandUnits(unitCount, unitIds);
  }

  @Contract(pure = true)
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

  @Contract(pure = true)
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

  @Contract(pure = true)
  private static Event.CommandData parseCommandData(LittleEndianDataInputStream stream) throws IOException {
    int commandId = stream.readInt();
    byte[] arg1 = stream.readNBytes(4);
    EventCommandType commandType = EventCommandType.values()[stream.readByte()];
    byte[] arg2 = stream.readNBytes(4);

    Event.CommandTarget commandTarget = parseCommandTarget(stream);

    byte[] arg3 = stream.readNBytes(1);

    Event.CommandFormation commandFormation = parseCommandFormation(stream);

    String blueprintId = LoadUtils.readString(stream);
    byte[] arg4 = stream.readNBytes(12);

    LuaData parametersLua = LoadUtils.parseLua(stream);
    boolean addToQueue = stream.readByte() > 0;

    return new Event.CommandData(
      commandId, commandType, commandTarget, commandFormation, blueprintId, parametersLua, addToQueue
    );
  }

  @Contract(pure = true)
  private static Event parseToken(ReplayBodyToken token) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(token.tokenContent())))) {
      Event event = switch (token.tokenId()) {
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
          String blueprintId = LoadUtils.readString(stream);
          float px = stream.readFloat();
          float pz = stream.readFloat();
          float heading = stream.readFloat();

          yield new Event.CreateUnit(playerIndex, blueprintId, px, pz, heading);
        }

        case CMDST_CREATE_PROP -> {
          String blueprintId = LoadUtils.readString(stream);
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
          int entityId = stream.readInt();
          String arg1 = LoadUtils.readString(stream);
          String arg2 = LoadUtils.readString(stream);
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
          LuaData parametersLua = LoadUtils.parseLua(stream);
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

        case CMDST_DEBUG_COMMAND -> {
          String command = LoadUtils.readString(stream);
          float px = stream.readFloat();
          float py = stream.readFloat();
          float pz = stream.readFloat();
          byte focusArmy = stream.readByte();
          Event.CommandUnits commandUnits = parseCommandUnits(stream);

          yield new Event.DebugCommand(command, px, py, pz, focusArmy, commandUnits);
        }

        case CMDST_EXECUTE_LUA_IN_SIM -> {
          String luaCode = LoadUtils.readString(stream);
          yield new Event.ExecuteLuaInSim(luaCode);
        }

        case CMDST_LUA_SIM_CALLBACK -> {
          String func = LoadUtils.readString(stream);
          LuaData args = LoadUtils.parseLua(stream);
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

      if(stream.available() > 0) {
        throw new IllegalStateException("Expected end of stream");
      }

      return event;
    }
  }

  private enum CommandTargetType {
    // Order is crucial
    NONE,
    ENTITY,
    POSITION
  }
}
