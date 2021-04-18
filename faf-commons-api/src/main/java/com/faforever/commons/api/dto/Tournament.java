package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.github.jasminb.jsonapi.annotations.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.WRAPPER_OBJECT)
public class Tournament {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  @ToString.Include
  private String name;
  private String description;
  @JsonProperty("tournament_type")
  private String tournamentType;
  @JsonProperty("created_at")
  private OffsetDateTime createdAt;
  @JsonProperty("participants_count")
  private int participantCount;
  @JsonProperty("start_at")
  private OffsetDateTime startingAt;
  @JsonProperty("completed_at")
  private OffsetDateTime completedAt;
  @JsonProperty("full_challonge_url")
  private String challongeUrl;
  @JsonProperty("live_image_url")
  private String liveImageUrl;
  @JsonProperty("sign_up_url")
  private String signUpUrl;
  @JsonProperty("open_signup")
  private boolean openForSignup;
}
