package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type(TeamkillReport.TYPE_NAME)
@RestrictedVisibility("IsModerator")
public class TeamkillReport extends AbstractEntity<TeamkillReport> {

  public static final String TYPE_NAME = "teamkillReport";

  /**
   * How many seconds into the game, in simulation time.
   */
  private Integer gameTime;
  @ToString.Include
  private OffsetDateTime reportedAt;

  @Relationship("teamkiller")
  private Player teamkiller;

  @Relationship("victim")
  private Player victim;

  @Relationship("game")
  @ToString.Include
  private Game game;
}
