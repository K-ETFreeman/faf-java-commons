package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@RestrictedVisibility("IsModerator")
@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("domainBlacklist")
public class DomainBlacklist implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String domain;

  @Override
  @JsonIgnore
  public String getId() {
    return domain;
  }
}
