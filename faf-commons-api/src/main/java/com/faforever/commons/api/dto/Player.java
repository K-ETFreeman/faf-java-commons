package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("player")
public class Player extends AbstractEntity {
  @ToString.Include
  private String login;
  @RestrictedVisibility("IsModerator")
  private String email;
  private String userAgent;
  @RestrictedVisibility("IsModerator")
  private String steamId;
  @RestrictedVisibility("IsModerator")
  private String recentIpAddress;
  @RestrictedVisibility("IsModerator")
  private OffsetDateTime lastLogin;

  @Relationship("names")
  private List<NameRecord> names;

  @Deprecated
  @Relationship("globalRating")
  private GlobalRating globalRating;

  @Deprecated
  @Relationship("ladder1v1Rating")
  private Ladder1v1Rating ladder1v1Rating;

  @Deprecated
  @Relationship("lobbyGroup")
  private LobbyGroup lobbyGroup;

  @Relationship("uniqueIds")
  @RestrictedVisibility("IsModerator")
  private Set<UniqueId> uniqueIds;

  @Relationship("bans")
  @RestrictedVisibility("IsModerator")
  private List<BanInfo> bans;

  @Relationship("avatarAssignments")
  @JsonIgnore
  private List<AvatarAssignment> avatarAssignments;

  @JsonBackReference
  @Relationship("reporterOnModerationReports")
  private Set<ModerationReport> reporterOnModerationReports;
}
