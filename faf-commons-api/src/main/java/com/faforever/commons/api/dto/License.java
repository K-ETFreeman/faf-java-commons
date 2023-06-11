package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("license")
public class License implements ElideEntity {

  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private String name;
  private String shortName;
  private String url;
  private String licenseText;
  private boolean active;
  private boolean revocable;
  private boolean redistributable;
  private boolean modifiable;
}

