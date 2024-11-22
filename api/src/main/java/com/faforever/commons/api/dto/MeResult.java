package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Set;

@Type(MeResult.TYPE_NAME)
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class MeResult implements ElideEntity {

  public static final String TYPE_NAME = "me";

  @Id
  private String id;
  @ToString.Include
  @EqualsAndHashCode.Include
  private String userId;
  @ToString.Include
  private String userName;
  private String email;
  private OffsetDateTime lastLogin;
  private Clan clan;
  private Set<String> groups;
  private Set<String> permissions;

  @Override
  public String getId() {
    return userId;
  }

  @Data
  public static class Clan {
    private Integer id;
    private Integer membershipId;
    private String tag;
    private String name;
  }
}
