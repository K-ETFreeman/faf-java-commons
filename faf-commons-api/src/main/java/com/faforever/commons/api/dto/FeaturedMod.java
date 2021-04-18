package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("featuredMod")
public class FeaturedMod implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  String id;
  String description;
  @ToString.Include
  String displayName;
  int order;
  String gitBranch;
  String gitUrl;
  String bireusUrl;
  @ToString.Include
  String technicalName;
  boolean visible;
}
