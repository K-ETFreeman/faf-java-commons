package com.faforever.commons.api.dto;


import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("playerAchievement")
public class PlayerAchievement extends AbstractEntity<PlayerAchievement> {
  @ToString.Include
  private AchievementState state;
  private Integer currentSteps;

  @Relationship("achievement")
  @ToString.Include
  private AchievementDefinition achievement;
}
