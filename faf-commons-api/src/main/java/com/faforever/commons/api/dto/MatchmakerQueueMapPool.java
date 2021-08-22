package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("matchmakerQueueMapPool")
public class MatchmakerQueueMapPool extends AbstractEntity<MatchmakerQueueMapPool> {
  @ToString.Include
  private Double minRating;
  @ToString.Include
  private Double maxRating;

  @Relationship("matchmakerQueue")
  @ToString.Include
  private MatchmakerQueue matchmakerQueue;

  @Relationship("mapPool")
  @JsonIgnore
  private MapPool mapPool;
}
