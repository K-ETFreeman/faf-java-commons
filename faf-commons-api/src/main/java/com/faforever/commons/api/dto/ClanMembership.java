package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("clanMembership")
public class ClanMembership extends AbstractEntity<ClanMembership> {
  @ToString.Include
  @Relationship("clan")
  private Clan clan;

  @ToString.Include
  @Relationship("player")
  private Player player;
}
