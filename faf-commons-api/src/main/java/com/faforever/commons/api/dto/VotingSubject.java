package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@Type(VotingSubject.TYPE_NAME)
public class VotingSubject extends AbstractEntity {
  public static final String TYPE_NAME = "votingSubject";

  private String subjectKey;
  private String subject;
  private Integer numberOfVotes;
  private String topicUrl;
  private OffsetDateTime beginOfVoteTime;
  private OffsetDateTime endOfVoteTime;
  private Integer minGamesToVote;
  private String descriptionKey;
  private String description;
  private Boolean revealWinner;
  @Relationship("votes")
  private List<Vote> votes;
  @Relationship("votingQuestions")
  private List<VotingQuestion> votingQuestions;
}

