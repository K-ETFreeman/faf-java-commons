package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leagueSeasonDivision")
public class LeagueSeasonDivision extends AbstractEntity<LeagueSeasonDivision> {
  private String descriptionKey;
  private Integer divisionIndex;
  @ToString.Include
  private String nameKey;

  @Relationship("leagueSeason")
  private LeagueSeason leagueSeason;
}
