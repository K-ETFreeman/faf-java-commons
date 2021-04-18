package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Set;

@Type("moderationReport")
@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ModerationReport extends AbstractEntity {
  private String reportDescription;
  private ModerationReportStatus reportStatus;
  private String gameIncidentTimecode;
  private String moderatorNotice;
  private String moderatorPrivateNote;

  @Relationship("bans")
  private Set<BanInfo> bans;
  @Relationship("reporter")
  private Player reporter;
  @Relationship("game")
  private Game game;
  @Relationship("lastModerator")
  private Player lastModerator;
  @Relationship("reportedUsers")
  private Set<Player> reportedUsers;
}
