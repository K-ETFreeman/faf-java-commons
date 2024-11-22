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
@Type(VotingQuestion.TYPE_NAME)
public class VotingQuestion extends AbstractEntity<VotingQuestion> {
  public static final String TYPE_NAME = "votingQuestion";

  private int numberOfAnswers;
  private String question;
  private String description;
  private String questionKey;
  private String descriptionKey;
  private Integer maxAnswers;
  private Integer ordinal;
  private Boolean alternativeQuestion;
  @Relationship("votingSubject")
  private VotingSubject votingSubject;
  @Relationship("winners")
  private List<VotingChoice> winners;
  @Relationship("votingChoices")
  private List<VotingChoice> votingChoices;

}
