package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.Duration;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("coopResult")
public class CoopResult {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private Duration duration;
  private String playerNames;
  private boolean secondaryObjectives;
  /**
   * This field is not provided by the API but must be enriched instead.
   */
  private int ranking;
  private int playerCount;

  @Relationship("game")
  private Game game;
}
