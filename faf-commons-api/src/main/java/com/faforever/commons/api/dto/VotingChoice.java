package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type(VotingChoice.TYPE_NAME)
public class VotingChoice extends AbstractEntity<VotingChoice> {
  public static final String TYPE_NAME = "votingChoice";

  private String choiceTextKey;
  private String choiceText;
  private String descriptionKey;
  private String description;
  private Integer numberOfAnswers;
  private Integer ordinal;
  @Relationship("votingQuestion")
  private VotingQuestion votingQuestion;
}
