package com.faforever.commons.api.dto;


import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Type("message")
public class Message implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private String key;
  private String language;
  private String region;
  private String value;
}
