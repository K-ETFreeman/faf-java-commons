package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type("mod")
public class Mod extends AbstractEntity<Mod> {
  private boolean recommended;
  @ToString.Include
  private String displayName;
  @ToString.Include
  private String author;

  @Relationship("uploader")
  @ToString.Include
  private Player uploader;

  @Relationship("versions")
  private List<ModVersion> versions;

  @Relationship("latestVersion")
  private ModVersion latestVersion;

  @Relationship("reviewsSummary")
  private ModReviewsSummary modReviewsSummary;
}
