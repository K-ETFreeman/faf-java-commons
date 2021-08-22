package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("mapPoolAssignment")
public class MapPoolAssignment extends AbstractEntity<MapPoolAssignment> {
  @ToString.Include
  int weight;
  @ToString.Include
  private MapParams mapParams;

  @Relationship("mapPool")
  @ToString.Include
  private MapPool mapPool;

  @Relationship("mapVersion")
  @ToString.Include
  private MapVersion mapVersion;
}


