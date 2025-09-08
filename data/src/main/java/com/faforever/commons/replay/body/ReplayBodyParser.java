package com.faforever.commons.replay.body;

import com.faforever.commons.replay.shared.LoadUtils;
import com.faforever.commons.replay.shared.LuaData;
import org.jetbrains.annotations.Contract;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

public class ReplayBodyParser {

  @Contract(pure = true)
  public static List<Event> parseTokens(List<ReplayBodyToken> tokens, ByteBuffer buffer) {
    return tokens.stream().map((token) -> {
      try {
        return parseToken(token, buffer);
      } catch (Exception exception) {
        return new Event.ProcessingError(token, exception);
      }
    }).toList();
  }

  @Contract(pure = true)
  private static Event.CommandUnits parseCommandUnits(ByteBuffer stream) {
    int unitCount = stream.getInt();
    ArrayList<Integer> unitIds = new ArrayList<>(unitCount);
    for (int k = 0; k < unitCount; k++) {
      unitIds.add(stream.getInt());
    }

    return new Event.CommandUnits(unitCount, unitIds);
  }

  @Contract(pure = true)
  private static Event.CommandFormation parseCommandFormation(ByteBuffer stream) {
    float orientation = 0;
    float px = 0;
    float py = 0;
    float pz = 0;
    float scale = 0;

    int formation = stream.getInt();
    if (formation != -1) {
      orientation = stream.getFloat();
      px = stream.getFloat();
      py = stream.getFloat();
      pz = stream.getFloat();
      scale = stream.getFloat();
    }

    return new Event.CommandFormation(formation, orientation, px, py, pz, scale);
  }

  @Contract(pure = true)
  private static Event.CommandTarget parseCommandTarget(ByteBuffer buffer) {
    CommandTargetType target = CommandTargetType.values()[buffer.get()];
    switch (target) {
      case ENTITY -> {
        int entityId = buffer.getInt();
        return new Event.CommandTarget.Entity(entityId);
      }

      case POSITION -> {
        float px = buffer.getFloat();
        float py = buffer.getFloat();
        float pz = buffer.getFloat();
        return new Event.CommandTarget.Position(px, py, pz);
      }

      default -> {
        return null;
      }
    }
  }

  @Contract(pure = true)
  private static Event.CommandData parseCommandData(ByteBuffer buffer) {
    int commandId = buffer.getInt();
    buffer.position(buffer.position() + 4);
    EventCommandType commandType = EventCommandType.values()[buffer.get()];
    buffer.position(buffer.position() + 4);


    Event.CommandTarget commandTarget = parseCommandTarget(buffer);

    buffer.position(buffer.position() + 1);


    Event.CommandFormation commandFormation = parseCommandFormation(buffer);

    String blueprintId = LoadUtils.readString(buffer);
    buffer.position(buffer.position() + 12);

    LuaData parametersLua = LoadUtils.parseLua(buffer);
    boolean addToQueue = buffer.get() > 0;

    return new Event.CommandData(
      commandId, commandType, commandTarget, commandFormation, blueprintId, parametersLua, addToQueue
    );
  }

  @Contract(pure = true)
  private static Event parseToken(final ReplayBodyToken token, final ByteBuffer buffer) {
    // Limit buffer to token end position
    buffer.limit(token.limit());
    // Skip header bytes
    buffer.position(buffer.position() + 3);
    Event event = switch (token.tokenId()) {
      case CMDST_ADVANCE -> {
        int ticks = buffer.getInt();
        yield new Event.Advance(ticks);
      }

      case CMDST_SET_COMMAND_SOURCE -> {
        int playerIndex = buffer.get();
        yield new Event.SetCommandSource(playerIndex);
      }

      case CMDST_COMMAND_SOURCE_TERMINATED -> new Event.CommandSourceTerminated();

      case CMDST_VERIFY_CHECKSUM -> {
        final byte[] hashBytes = new byte[16];
        buffer.get(hashBytes);
        String hash = HexFormat.of().formatHex(hashBytes);
        int tick = buffer.getInt();

        yield new Event.VerifyChecksum(hash, tick);
      }

      case CMDST_REQUEST_PAUSE -> new Event.RequestPause();

      case CMDST_RESUME -> new Event.RequestResume();

      case CMDST_SINGLE_STEP -> new Event.SingleStep();

      case CMDST_CREATE_UNIT -> {
        int playerIndex = buffer.get();
        String blueprintId = LoadUtils.readString(buffer);
        float px = buffer.getFloat();
        float pz = buffer.getFloat();
        float heading = buffer.getFloat();

        yield new Event.CreateUnit(playerIndex, blueprintId, px, pz, heading);
      }

      case CMDST_CREATE_PROP -> {
        String blueprintId = LoadUtils.readString(buffer);
        float px = buffer.getFloat();
        float pz = buffer.getFloat();
        float heading = buffer.getFloat();

        yield new Event.CreateProp(blueprintId, px, pz, heading);
      }

      case CMDST_DESTROY_ENTITY -> {
        int entityId = buffer.getInt();
        yield new Event.DestroyEntity(entityId);
      }

      case CMDST_WARP_ENTITY -> {
        int entityId = buffer.getInt();
        float px = buffer.getFloat();
        float py = buffer.getFloat();
        float pz = buffer.getFloat();
        yield new Event.WarpEntity(entityId, px, py, pz);
      }

      case CMDST_PROCESS_INFO_PAIR -> {
        int entityId = buffer.getInt();
        String arg1 = LoadUtils.readString(buffer);
        String arg2 = LoadUtils.readString(buffer);
        yield new Event.ProcessInfoPair(entityId, arg1, arg2);
      }

      case CMDST_ISSUE_COMMAND -> {
        Event.CommandUnits commandUnits = parseCommandUnits(buffer);
        Event.CommandData commandData = parseCommandData(buffer);

        yield new Event.IssueCommand(commandUnits, commandData);
      }

      case CMDST_ISSUE_FACTORY_COMMAND -> {
        Event.CommandUnits commandUnits = parseCommandUnits(buffer);
        Event.CommandData commandData = parseCommandData(buffer);

        yield new Event.IssueFactoryCommand(commandUnits, commandData);
      }

      case CMDST_INCREASE_COMMAND_COUNT -> {
        int commandId = buffer.getInt();
        int delta = buffer.getInt();
        yield new Event.IncreaseCommandCount(commandId, delta);
      }

      case CMDST_DECRASE_COMMAND_COUNT -> {
        int commandId = buffer.getInt();
        int delta = buffer.getInt();
        yield new Event.DecreaseCommandCount(commandId, delta);
      }

      case CMDST_SET_COMMAND_TARGET -> {
        int commandId = buffer.getInt();
        Event.CommandTarget commandTarget = parseCommandTarget(buffer);
        yield new Event.SetCommandTarget(commandId, commandTarget);
      }

      case CMDST_SET_COMMAND_TYPE -> {
        int commandId = buffer.getInt();
        int targetCommandType = buffer.getInt();
        yield new Event.SetCommandType(commandId, targetCommandType);
      }

      case CMDST_SET_COMMAND_CELLS -> {
        int commandId = buffer.getInt();
        LuaData parametersLua = LoadUtils.parseLua(buffer);
        if (!(parametersLua instanceof LuaData.Nil)) {
          buffer.get();
        }

        float px = buffer.getFloat();
        float py = buffer.getFloat();
        float pz = buffer.getFloat();

        yield new Event.SetCommandCells(commandId, parametersLua, px, py, pz);
      }

      case CMDST_REMOVE_COMMAND_FROM_QUEUE -> {
        int commandId = buffer.getInt();
        int unitId = buffer.getInt();
        yield new Event.RemoveCommandFromQueue(commandId, unitId);
      }

      case CMDST_DEBUG_COMMAND -> {
        String command = LoadUtils.readString(buffer);
        float px = buffer.getFloat();
        float py = buffer.getFloat();
        float pz = buffer.getFloat();
        byte focusArmy = buffer.get();
        Event.CommandUnits commandUnits = parseCommandUnits(buffer);

        yield new Event.DebugCommand(command, px, py, pz, focusArmy, commandUnits);
      }

      case CMDST_EXECUTE_LUA_IN_SIM -> {
        String luaCode = LoadUtils.readString(buffer);
        yield new Event.ExecuteLuaInSim(luaCode);
      }

      case CMDST_LUA_SIM_CALLBACK -> {
        String func = LoadUtils.readString(buffer);
        LuaData args = LoadUtils.parseLua(buffer);
        Event.CommandUnits commandUnits = null;

        // suspicion that this is just flat out wrong! Whether there's a selection in the data is not related to whether there are Lua arguments
        if (!(args instanceof LuaData.Nil)) {
          commandUnits = parseCommandUnits(buffer);
        } else {
          // the '4' we read here is the size, I suspect the 3 bytes are maybe to align the data somehow? No idea
          buffer.position(buffer.position() + 4 + 3);
        }

        yield new Event.LuaSimCallback(func, args, commandUnits);
      }

      case CMDST_END_GAME -> new Event.EndGame();

      case null -> new Event.Unprocessed(token, "Unknown");
    };

    // If we are still not at the supposed end of the token calculated from the header, set position to the limit.
    if (buffer.position() != token.limit()) {
      buffer.position(token.limit());
      throw new IllegalStateException("Expected end of token");
    }

    return event;
  }

  private enum CommandTargetType {
    // Order is crucial
    NONE,
    ENTITY,
    POSITION
  }
}
