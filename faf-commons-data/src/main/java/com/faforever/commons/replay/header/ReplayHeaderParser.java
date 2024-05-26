package com.faforever.commons.replay.header;

import com.faforever.commons.replay.shared.LoadUtils;
import com.faforever.commons.replay.shared.LuaData;
import com.google.common.io.LittleEndianDataInputStream;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

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
    GameScenario gameScenario = parseGameScenario((gameOptionBytes));

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
        byte[] arg3 = dataStream.readNBytes(1); // always -1
      }
    }

    int seed = dataStream.readInt();

    return new ReplayHeader(gameVersion, replayVersion, pathToScenario, cheatsEnabled, seed, sources, mods, gameScenario, allPlayerOptions);
  }

  @Contract(pure = true)
  private static @Nullable List<GameMod> parseMod(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData modInfo = parseLua(stream);

      if (modInfo instanceof LuaData.Table table) {
        return table.value().values().stream().map(
          e -> {
            if (e instanceof LuaData.Table luaModInfo) {
              return new GameMod(
                luaModInfo.getString("location"),
                luaModInfo.getString("icon"),
                luaModInfo.getString("copyright"),
                luaModInfo.getString("name"),
                luaModInfo.getString("description"),
                luaModInfo.getString("author"),
                luaModInfo.getString("uid"),
                luaModInfo.getInteger("version"),
                luaModInfo.getString("url")
              );
            }

            return null;
          }
        ).toList();
      }

      return null;
    }
  }

  @Contract(pure = true)
  private static @Nullable GameScenario parseGameScenario(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData gameScenario = parseLua(stream);

      if (gameScenario instanceof LuaData.Table table) {

        // retrieve and manage the game options
        GameOptions gameOptions = null;
        Map<String, String> modOptions = null;
        if (table.value().get("Options") instanceof LuaData.Table optionsTable) {

          gameOptions = new GameOptions(
            GameOptions.AutoTeams.findByKey(optionsTable.getString("AutoTeams")),
            GameOptions.TeamLock.findByKey(optionsTable.getString("TeamLock")),
            GameOptions.TeamSpawn.findByKey(optionsTable.getString("TeamSpawn")),
            optionsTable.getBool("AllowObservers"),
            optionsTable.getBool("CheatsEnabled"),
            optionsTable.getBool("PrebuiltUnits"),
            optionsTable.getBool("RevealCivilians"),
            optionsTable.getBool("Score"),
            optionsTable.getInteger("UnitCap"),
            optionsTable.getString("Unranked"),
            GameOptions.Victory.findByKey(optionsTable.getString("Victory"))
          );

          optionsTable.removeKey("AutoTeams");
          optionsTable.removeKey("TeamLock");
          optionsTable.removeKey("TeamSpawn");
          optionsTable.removeKey("AllowObservers");
          optionsTable.removeKey("CheatsEnabled");
          optionsTable.removeKey("PrebuiltUnits");
          optionsTable.removeKey("RevealCivilians");
          optionsTable.removeKey("Score");
          optionsTable.removeKey("UnitCap");
          optionsTable.removeKey("Unranked");
          optionsTable.removeKey("Victory");

          modOptions = optionsTable.toKeyStringValuePairs();
        }

        Integer sizeX = null;
        Integer sizeZ = null;
        if (table.getTable("size") instanceof LuaData.Table sizeTable) {
          sizeX = sizeTable.getInteger("1.0");
          sizeZ = sizeTable.getInteger("2.0");
        }

        Integer massReclaimValue = null;
        Integer energyReclaimValue = null;
        if (table.value().get("reclaim") instanceof LuaData.Table reclaimTable) {
          massReclaimValue = reclaimTable.getInteger("1.0");
          energyReclaimValue = reclaimTable.getInteger("2.0");
        }

        return new GameScenario(
          table.getString("map"),
          table.getInteger("map_version"),
          table.getString("description"),
          table.getString("script"),
          table.getString("save"),
          table.getString("name"),
          sizeX, sizeZ,
          massReclaimValue, energyReclaimValue,
          gameOptions, modOptions
        );

      }

      return null;
    }
  }

  @Contract(pure = true)
  private static @Nullable PlayerOptions parsePlayerOptions(byte[] bytes) throws IOException {
    try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream((new ByteArrayInputStream(bytes)))) {
      LuaData playerOptions = parseLua(stream);

      if (playerOptions instanceof LuaData.Table table) {
        return new PlayerOptions(
          table.getBool("Human"),
          table.getString("AIPersonality"),
          table.getFloat("MEAN"),
          table.getFloat("DEV"),
          table.getString("PlayerClan"),
          table.getBool("Civilian"),
          table.getInteger("StartSpot"),
          table.getString("ArmyName"),
          table.getInteger("ArmyColor"),
          table.getInteger("PlayerColor"),
          table.getString("PlayerName"),
          table.getInteger("NG"),
          table.getString("Country"),
          table.getInteger("Team"),
          table.getInteger("Faction")
        );
      }

      return null;
    }
  }
}
