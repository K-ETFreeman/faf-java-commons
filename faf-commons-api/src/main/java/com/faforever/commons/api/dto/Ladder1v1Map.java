package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Deprecated
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("ladder1v1Map")
public class Ladder1v1Map implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @Relationship("mapVersion")
  private MapVersion mapVersion;
}
