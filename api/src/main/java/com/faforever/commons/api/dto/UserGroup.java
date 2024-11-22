package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type(UserGroup.TYPE_NAME)
public class UserGroup extends AbstractEntity<UserGroup> {

  public static final String TYPE_NAME = "userGroup";

  @ToString.Include
  private String technicalName;
  @ToString.Include
  private String nameKey;
  @JsonProperty("public")
  private boolean public_;
  @Relationship("members")
  @JsonIgnore
  private Set<Player> members;
  @Relationship("permissions")
  private Set<GroupPermission> permissions;
  @Relationship("children")
  private Set<UserGroup> children;
  @Relationship("parent")
  @JsonIgnore
  private UserGroup parent;
}
