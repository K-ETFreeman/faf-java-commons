package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @deprecated LobbyGroups are supposed to be replaced with role based security
 */

@Deprecated
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("lobbyGroup")
public class LobbyGroup {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String userId;
  private LegacyAccessLevel accessLevel;
}
