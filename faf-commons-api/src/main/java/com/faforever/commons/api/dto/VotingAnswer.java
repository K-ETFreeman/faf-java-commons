package com.faforever.commons.api.dto;


import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type(VotingAnswer.TYPE_NAME)
public class VotingAnswer extends AbstractEntity {
  public static final String TYPE_NAME = "votingAnswer";

  @Relationship("vote")
  private Vote vote;
  private Integer alternativeOrdinal;
  @Relationship("votingChoice")
  private VotingChoice votingChoice;
}
