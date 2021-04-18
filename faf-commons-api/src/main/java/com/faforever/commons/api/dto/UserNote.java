package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("userNote")
@RestrictedVisibility("IsModerator")
public class UserNote extends AbstractEntity {
  @Relationship("player")
  private Player player;
  @Relationship("author")
  private Player author;
  private boolean watched;
  private String note;
}
