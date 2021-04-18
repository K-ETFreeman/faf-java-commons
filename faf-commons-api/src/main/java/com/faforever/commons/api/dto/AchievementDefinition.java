package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Type("achievement")
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AchievementDefinition implements ElideEntity {

  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private String description;
  private int experiencePoints;
  private AchievementState initialState;
  @ToString.Include
  private String name;
  private String revealedIconUrl;
  private Integer totalSteps;
  @ToString.Include
  private AchievementType type;
  private String unlockedIconUrl;
  private int order;
}
