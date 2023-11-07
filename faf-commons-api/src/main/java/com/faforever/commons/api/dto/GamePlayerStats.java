package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("gamePlayerStats")
public class GamePlayerStats implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private boolean ai;
  private Faction faction;
  private byte color;
  private byte team;
  private byte startSpot;
  @Deprecated
  private Float beforeMean;
  @Deprecated
  private Float beforeDeviation;
  @Deprecated
  private Float afterMean;
  @Deprecated
  private Float afterDeviation;
  private byte score;
  @Nullable
  private OffsetDateTime scoreTime;
  @ToString.Include
  private GameOutcome result;

  @Relationship("game")
  @JsonIgnore
  private Game game;

  @Relationship("player")
  private Player player;

  @Relationship("ratingChanges")
  private List<LeaderboardRatingJournal> leaderboardRatingJournals;
}
