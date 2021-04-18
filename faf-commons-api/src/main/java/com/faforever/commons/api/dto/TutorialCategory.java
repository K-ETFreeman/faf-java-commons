package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@Type("tutorialCategory")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TutorialCategory implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private String categoryKey;
  @ToString.Include
  private String category;
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @Relationship("tutorials")
  private List<Tutorial> tutorials;

}
