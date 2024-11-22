package com.faforever.commons.api.dto;


import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@Deprecated
public class Rating {
  @Id
  @ToString.Include
  @EqualsAndHashCode.Include
  private String id;
  private double mean;
  private double deviation;
  @ToString.Include
  private double rating;

  @Relationship("player")
  private Player player;
}
