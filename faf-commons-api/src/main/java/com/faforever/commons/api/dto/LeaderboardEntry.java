package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leaderboardRating")
public class LeaderboardEntry extends AbstractEntity<LeaderboardEntry> {
  @ToString.Include
  private Double mean;
  @ToString.Include
  private Double deviation;
  private Integer totalGames;
  private Integer wonGames;
  private Double rating;

  @Relationship("player")
  @ToString.Include
  private Player player;

  @Relationship("leaderboard")
  @ToString.Include
  private Leaderboard leaderboard;
}
