package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("tutorial")
public class Tutorial extends AbstractEntity {
  private String descriptionKey;
  private String description;
  private String titleKey;
  @ToString.Include
  private String title;
  @Relationship("category")
  private TutorialCategory category;
  private String image;
  private String imageUrl;
  private int ordinal;
  private boolean launchable;
  @ToString.Include
  private String technicalName;
  @Relationship("mapVersion")
  private MapVersion mapVersion;
}
