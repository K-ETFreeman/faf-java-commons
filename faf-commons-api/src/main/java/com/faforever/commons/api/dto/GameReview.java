package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Type("gameReview")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GameReview extends Review {
  @Relationship("game")
  private Game game;
}
