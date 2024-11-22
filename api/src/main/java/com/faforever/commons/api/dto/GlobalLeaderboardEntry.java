package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @deprecated GlobaleaderboardEntry replaced with leaderboardRating
 */
@Deprecated
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Type("globalLeaderboardEntry")
public class GlobalLeaderboardEntry {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private String name;
  private int rank;
  private Double mean;
  private Double deviation;
  private Integer numGames;
  private Boolean isActive;
  private Double rating;
}
