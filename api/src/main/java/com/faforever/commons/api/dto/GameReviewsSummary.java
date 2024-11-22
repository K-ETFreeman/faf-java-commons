package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Type("gameReviewsSummary")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GameReviewsSummary extends ReviewsSummary {
  @Relationship("game")
  private Game game;
}
