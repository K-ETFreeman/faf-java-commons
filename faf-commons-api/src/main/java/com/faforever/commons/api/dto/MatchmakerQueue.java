package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("matchmakerQueue")
public class MatchmakerQueue extends AbstractEntity {
  @ToString.Include
  private String technicalName;
  @ToString.Include
  private String nameKey;

  @Relationship("featuredMod")
  private FeaturedMod featuredMod;

  @Relationship("leaderboard")
  @ToString.Include
  private Leaderboard leaderboard;
}
