package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("uniqueIdAssignment")
@RestrictedVisibility("IsModerator")
public class UniqueIdAssignment extends AbstractEntity<UniqueIdAssignment> {

  @Relationship("player")
  private Player player;

  @Relationship("uniqueId")
  private UniqueId uniqueId;
}
