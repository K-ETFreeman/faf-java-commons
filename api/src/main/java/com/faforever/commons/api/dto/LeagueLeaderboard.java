package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leagueLeaderboard")
public class LeagueLeaderboard extends AbstractEntity<LeagueLeaderboard> {
  private String technicalName;
}
