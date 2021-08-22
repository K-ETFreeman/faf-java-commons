package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leagueSeasonDivisionSubdivision")
public class LeagueSeasonDivisionSubdivision extends AbstractEntity<LeagueSeasonDivisionSubdivision> {
  private String descriptionKey;
  private Integer highestScore;
  private Integer maxRating;
  private Integer minRating;
  @ToString.Include
  private String nameKey;
  private Integer subdivisionIndex;

  @Relationship("leagueSeasonDivision")
  private LeagueSeasonDivision leagueSeasonDivision;
}
