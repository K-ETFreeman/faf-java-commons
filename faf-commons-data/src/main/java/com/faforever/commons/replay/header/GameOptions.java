package com.faforever.commons.replay.header;

import java.util.Arrays;

/**
 * Populated by the game options field of the table that is passed to `CLobby:LaunchGame`
 * @param autoTeams
 * @param teamLock
 * @param teamSpawn
 * @param allowObservers
 * @param cheatsEnabled
 * @param prebuiltUnits
 * @param revealedCivilians
 * @param scoreEnabled
 * @param unitCap
 * @param unRated
 * @param victory
 */
public record GameOptions(AutoTeams autoTeams, TeamLock teamLock, TeamSpawn teamSpawn, boolean allowObservers,
                          boolean cheatsEnabled, boolean prebuiltUnits, boolean revealedCivilians, boolean scoreEnabled,
                          int unitCap, boolean unRated, Victory victory) {

  public enum AutoTeams {
    NONE("none", "None"),
    MANUAL("manual", "Manual"),
    TOP_VS_BOTTOM("tvsb", "Top versus bottom"),
    LEFT_VS_RIGHT("lvsr", "Left versus right"),
    EVEN_VS_UNEVEN("pvsi", "Even versus uneven");

    public final String readable;

    private final String key;

    AutoTeams(String key, String readable) {
      this.readable = readable;
      this.key = key;
    }

    public static AutoTeams findByKey(String key) {
      for (AutoTeams autoTeam : values()) {
        if (autoTeam.key.equals(key)) {
          return autoTeam;
        }
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }
  }

  public enum TeamLock {
    LOCKED("locked", "Locked"), UNLOCKED("unlocked", "Unlocked");

    public final String readable;

    private final String key;

    TeamLock(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static TeamLock findByKey(String key) {
      for (TeamLock teamLock : values()) {
        if (teamLock.key.equals(key)) {
          return teamLock;
        }
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }
  }


  public enum TeamSpawn {
    FIXED("fixed", "Fixed"),
    RANDOM("random", "Random"),
    BALANCED("balanced", "Balanced"),
    BALANCED_FLEX("balanced_flex", "Flexible balanced"),
    RANDOM_REVEAL("random_reveal", "Random and revealed"),
    BALANCED_REVEAL("balanced_reveal", "Balanced and revealed"),
    BALANCED_REVEAL_MIRRORED("balanced_reveal_mirrored", "Mirror balanced and revealed"),
    BALANCED_FLEX_REVEAL("balanced_flex_reveal", "Flexible balanced and revealed");

    public final String readable;

    private final String key;

    TeamSpawn(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static TeamSpawn findByKey(String key) {
      for (TeamSpawn teamSpawn : values()) {
        if (teamSpawn.key.equals(key)) {
          return teamSpawn;
        }
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }
  }

  public enum Victory {
    DEMORALIZATION("demoralization", "Assasination"),
    DOMINATION("domination", "Supremacy"),
    ERADICATION("eradication", "Annihilation"),
    SANDBOX("sandbox", "Sandbox");

    public final String readable;

    private final String key;

    Victory(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static Victory findByKey(String key) {
      for (Victory victory : values()) {
        if (victory.key.equals(key)) {
          return victory;
        }
      }
      throw new IllegalArgumentException("Unknown key: " + key);
    }
  }

}
