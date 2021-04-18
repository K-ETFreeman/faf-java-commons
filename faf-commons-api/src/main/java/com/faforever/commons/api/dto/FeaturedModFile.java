package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Type("featuredModFile")
public class FeaturedModFile {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private String version;
  private String group;
  @ToString.Include
  private String name;
  @ToString.Include
  private String md5;
  @ToString.Include
  private String url;
}
