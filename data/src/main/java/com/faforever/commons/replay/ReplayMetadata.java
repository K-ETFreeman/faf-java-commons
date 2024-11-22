package com.faforever.commons.replay;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * This class is meant to be serialized/deserialized from/to JSON.
 */
@Data
public class ReplayMetadata {
  private CompressionType compression;
  private String host;
  private Integer uid;
  private String title;
  private String mapname;
  private GameStatus state;
  //TODO what is this?
  private Boolean[] options;
  @JsonProperty("game_type")
  private VictoryCondition victoryCondition;
  @JsonProperty("featured_mod")
  private String featuredMod;
  @JsonProperty("max_players")
  private Integer maxPlayers;
  @JsonProperty("num_players")
  private Integer numPlayers;
  @JsonProperty("sim_mods")
  private Map<String, String> simMods;
  private Map<String, List<String>> teams;
  @JsonProperty("featured_mod_versions")
  private Map<String, Integer> featuredModVersions;
  private boolean complete;
  private String recorder;
  @JsonProperty("version_info")
  private Map<String, String> versionInfo;
  @JsonProperty("game_end")
  private double gameEnd;
  /**
   * Backwards compatibility: If 0.0, then {@code launchedAt} should be available instead.
   */
  @JsonProperty("time")
  private double gameTime;
  /**
   * Backwards compatibility: If 0.0, then {@code gameTime} should be available instead.
   */
  @JsonProperty("launched_at")
  private double launchedAt;

  @JsonSetter("compression")
  public void setCompressionType(String compressionType) {
    compression = CompressionType.fromString(compressionType);
  }

  @JsonSetter("game_type")
  public void setVictoryCondition(Object victoryCondition) {
    this.victoryCondition = VictoryCondition.fromNumber(victoryCondition);
  }

  @JsonSetter("state")
  public void setState(String state) {
    this.state = GameStatus.fromString(state);
  }
}
