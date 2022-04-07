package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leagueSeasonScore")
public class LeagueSeasonScore extends AbstractEntity<LeagueSeasonScore> {
  private Integer gameCount;
  private Integer loginId;
  private Integer score;
  private Boolean returningPlayer;

  @Relationship("leagueSeason")
  private LeagueSeason leagueSeason;

  @Relationship("leagueSeasonDivisionSubdivision")
  private LeagueSeasonDivisionSubdivision leagueSeasonDivisionSubdivision;
}
