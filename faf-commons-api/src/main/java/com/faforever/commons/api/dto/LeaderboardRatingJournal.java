package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leaderboardRatingJournal")
public class LeaderboardRatingJournal extends AbstractEntity<LeaderboardRatingJournal> {
  private Double meanAfter;
  private Double deviationAfter;
  private Double meanBefore;
  private Double deviationBefore;

  @Relationship("gamePlayerStats")
  @ToString.Include
  private GamePlayerStats gamePlayerStats;

  @Relationship("leaderboard")
  @ToString.Include
  private Leaderboard leaderboard;
}
