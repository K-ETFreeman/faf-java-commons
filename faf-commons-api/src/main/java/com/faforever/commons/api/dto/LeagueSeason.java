package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leagueSeason")
public class LeagueSeason extends AbstractEntity<LeagueSeason> {
  @ToString.Include
  private String nameKey;
  private Integer seasonNumber;
  private Integer placementGames;
  private OffsetDateTime startDate;
  private OffsetDateTime endDate;

  @Relationship("league")
  private League league;

  @Relationship("leaderboard")
  private LeagueLeaderboard leagueLeaderboard;
}
