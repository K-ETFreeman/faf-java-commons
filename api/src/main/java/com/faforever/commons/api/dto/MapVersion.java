package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("mapVersion")
public class MapVersion extends AbstractEntity<MapVersion> {
  private String description;
  private Integer maxPlayers;
  private Integer width;
  private Integer height;
  private Integer gamesPlayed;
  private ComparableVersion version;
  @ToString.Include
  private String folderName;
  // TODO name consistently with folderName
  private String filename;
  private Boolean ranked;
  private Boolean hidden;
  private URL thumbnailUrlSmall;
  private URL thumbnailUrlLarge;
  private URL downloadUrl;

  @Relationship("map")
  private Map map;

  @Relationship("statistics")
  @Deprecated
  private MapVersionStatistics statistics;

  @Deprecated
  @Nullable
  @Relationship("ladder1v1Map")
  private Ladder1v1Map ladder1v1Map;

  @Relationship("reviews")
  private List<MapVersionReview> reviews;

  @Relationship("reviewsSummary")
  private MapVersionReviewsSummary mapVersionReviewsSummary;
}
