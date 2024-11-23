package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("leaderboard")
public class Leaderboard extends AbstractEntity<Leaderboard> {
  @ToString.Include
  private String technicalName;
  @ToString.Include
  private String nameKey;
  private String descriptionKey;
}
