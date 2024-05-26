package com.faforever.commons.replay.header;

import java.util.Arrays;

/**
 * Populated by the game options field of the table that is passed to `CLobby:LaunchGame`
 *
 * @param autoTeams
 * @param teamLock
 * @param teamSpawn
 * @param allowObservers
 * @param cheatsEnabled
 * @param prebuiltUnits
 * @param revealedCivilians
 * @param scoreEnabled
 * @param unitCap
 * @param unRanked
 * @param victory
 */
public record GameOptions(AutoTeams autoTeams, TeamLock teamLock, TeamSpawn teamSpawn, Boolean allowObservers,
                          Boolean cheatsEnabled, Boolean prebuiltUnits, Boolean revealedCivilians, Boolean scoreEnabled,
                          Integer unitCap, String unRanked, Victory victory) {

  public enum AutoTeams {
    NONE("none", "None"),
    MANUAL("manual", "Manual"),
    TOP_VS_BOTTOM("tvsb", "Top versus bottom"),
    LEFT_VS_RIGHT("lvsr", "Left versus right"),
    EVEN_VS_UNEVEN("pvsi", "Even versus uneven"),
    UNKNOWN("unknown", "Unknown");

    public final String readable;

    private final String key;

    AutoTeams(String key, String readable) {
      this.readable = readable;
      this.key = key;
    }

    public static AutoTeams findByKey(String key) {
      for (AutoTeams value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }
      return AutoTeams.UNKNOWN;
    }
  }

  public enum TeamLock {
    LOCKED("locked", "Locked"), UNLOCKED("unlocked", "Unlocked"), UNKNOWN("unknown", "Unknown");;

    public final String readable;

    private final String key;

    TeamLock(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static TeamLock findByKey(String key) {
      for (TeamLock value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }

      return TeamLock.UNKNOWN;
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
    BALANCED_FLEX_REVEAL("balanced_flex_reveal", "Flexible balanced and revealed"),
    UNKNOWN("unknown", "Unknown");

    public final String readable;

    private final String key;

    TeamSpawn(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static TeamSpawn findByKey(String key) {
      for (TeamSpawn value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }

      return TeamSpawn.UNKNOWN;
    }
  }

  public enum Victory {
    DEMORALIZATION("demoralization", "Assasination"),
    DOMINATION("domination", "Supremacy"),
    ERADICATION("eradication", "Annihilation"),
    SANDBOX("sandbox", "Sandbox"),
    UNKNOWN("unknown", "Unknown");

    public final String readable;

    private final String key;

    Victory(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static Victory findByKey(String key) {
      for (Victory value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }

      return Victory.UNKNOWN;
    }
  }

  public enum SharedArmy {
    OFF("demoralization", "Assasination"),
    UNION("domination", "Supremacy"),
    COMMON("eradication", "Annihilation"),
    UNKNOWN("unknown", "Unknown");

    public final String readable;

    private final String key;

    SharedArmy(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static SharedArmy findByKey(String key) {
      for (SharedArmy value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }

      return SharedArmy.UNKNOWN;
    }
  }

  public enum Share {
    FULLSHARE("FullShare", "Full share"),
    SHAREUNTILDEATH("ShareUntilDeath", "Share until death"),
    PARTIALSHARE("PartialShare", "Partial share"),
    TRAITORS("TransferToKiller", "Traitors"),
    DEFECTORS("Defectors", "Defectors"),
    DESERTION("CivilianDeserter", "Desert to civilians"),
    UNKNOWN("unknown", "Unknown");

    public final String readable;

    private final String key;

    Share(String key, String readable) {
      this.key = key;
      this.readable = readable;
    }

    public static Share findByKey(String key) {
      for (Share value : values()) {
        if (value.key.equals(key)) {
          return value;
        }
      }

      return Share.UNKNOWN;
    }
  }
}
