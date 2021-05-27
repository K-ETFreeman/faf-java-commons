package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@Type("map")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Map extends AbstractEntity {
  private String battleType;
  @ToString.Include
  private String displayName;
  private String mapType;
  private Integer gamesPlayed;

  @Relationship("author")
  @ToString.Include
  private Player author;

  @Relationship("statistics")
  @Deprecated
  private MapStatistics statistics;

  @Relationship("latestVersion")
  @JsonIgnore
  private MapVersion latestVersion;

  @Relationship("versions")
  @JsonIgnore
  private List<MapVersion> versions;

  @Relationship("reviewsSummary")
  private MapReviewsSummary mapReviewsSummary;
}
