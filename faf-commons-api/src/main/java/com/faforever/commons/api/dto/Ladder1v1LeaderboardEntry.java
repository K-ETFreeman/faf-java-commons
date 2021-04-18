package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @deprecated Ladder1v1LeaderboardEntry replaced with leaderboardRating
 */
@Deprecated
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("ladder1v1LeaderboardEntry")
public class Ladder1v1LeaderboardEntry {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private int rank;
  @ToString.Include
  private String name;
  private Double mean;
  private Double deviation;
  private Integer numGames;
  private Integer wonGames;
  private Boolean isActive;
  private Double rating;
}
