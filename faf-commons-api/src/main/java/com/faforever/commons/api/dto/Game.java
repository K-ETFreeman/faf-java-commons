package com.faforever.commons.api.dto;

import com.faforever.commons.api.elide.ElideEntity;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Type("game")
public class Game implements ElideEntity {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private String name;
  private Boolean replayAvailable;
  private OffsetDateTime startTime;
  private OffsetDateTime endTime;
  private Integer replayTicks;
  private Validity validity;
  private VictoryCondition victoryCondition;

  @Relationship("reviews")
  private List<GameReview> reviews;

  @Relationship("playerStats")
  private List<GamePlayerStats> playerStats;

  @Relationship("host")
  private Player host;

  @Relationship("featuredMod")
  private FeaturedMod featuredMod;

  @Relationship("mapVersion")
  private MapVersion mapVersion;

  @Relationship("reviewsSummary")
  private GameReviewsSummary gameReviewsSummary;
}
