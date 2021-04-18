package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Review extends AbstractEntity {
  private String text;
  private Byte score;

  @Relationship("player")
  @ToString.Include
  private Player player;
}
