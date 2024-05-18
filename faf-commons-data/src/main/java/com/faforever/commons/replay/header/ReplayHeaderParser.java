package com.faforever.commons.replay.header;

import com.faforever.commons.replay.shared.LoadUtils;
import com.faforever.commons.replay.shared.LuaData;
import com.google.common.io.LittleEndianDataInputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.faforever.commons.replay.shared.LoadUtils.parseLua;

public class ReplayHeaderParser {

  @Contract(pure = true)
  public static ReplayHeader parse(LittleEndianDataInputStream dataStream) throws IOException {

    String gameVersion = LoadUtils.readString(dataStream);
    String arg1 = LoadUtils.readString(dataStream); // Always \r\n

    String[] replayAndScenario = LoadUtils.readString(dataStream).split("\\r\\n");
    String replayVersion = replayAndScenario[0];
    String pathToScenario = replayAndScenario[1];
    String arg2 = LoadUtils.readString(dataStream); // always \r\n and some unknown character

    int sizeModsInBytes = dataStream.readInt();
    byte[] modBytes = dataStream.readNBytes(sizeModsInBytes);
    List<GameMod> mods = parseMod(modBytes);

    int sizeGameOptionsInBytes = dataStream.readInt();
    byte[] gameOptionBytes = dataStream.readNBytes(sizeGameOptionsInBytes);
    GameOptions gameOptions = parseGameOptions((gameOptionBytes));

    int numberOfClients = dataStream.readUnsignedByte();
    List<Source> sources = new ArrayList<>(numberOfClients);
    for (int i = 0; i < numberOfClients; i++) {
      String playerName = LoadUtils.readString(dataStream);
      int playerId = dataStream.readInt();
      Source source = new Source(i, playerId, playerName);
      sources.add(source);
    }

    boolean cheatsEnabled = dataStream.readUnsignedByte() > 0;

    int numberOfArmies = dataStream.readUnsignedByte();
    List<PlayerOptions> allPlayerOptions = new ArrayList<>(numberOfClients);
    for (int i = 0; i < numberOfArmies; i++) {
      int sizePlayerOptionsInBytes = dataStream.readInt();
      byte[] playerOptionsBytes = dataStream.readNBytes(sizePlayerOptionsInBytes );
      PlayerOptions playerOptions = parsePlayerOptions(playerOptionsBytes);

      int playerSource = dataStream.readUnsignedByte();
      allPlayerOptions.add(playerOptions);

      if (playerSource != 255) {
        byte[] arg3 = dataStream.readNBytes(1);
      }
    }

    int seed = dataStream.readInt();

    return new ReplayHeader(gameVersion, replayVersion, pathToScenario, cheatsEnabled, seed, sources, mods, gameOptions, allPlayerOptions);
  }

  @Contract(pure = true)
  private static @Nullable List<GameMod> parseMod(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData mod = parseLua(stream);

      // TODO: needs implementation

      return null;
    }
  }

  @Contract(pure = true)
  private static @Nullable GameOptions parseGameOptions(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData gameOptions = parseLua(stream);

      // TODO: needs implementation

      return null;
    }
  }

  @Contract(pure = true)
  private static @Nullable PlayerOptions parsePlayerOptions(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData playerOptions = parseLua(stream);

      // TODO: needs implementation

      return null;
    }
  }
}
