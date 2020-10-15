package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Type("matchmakerQueueMapPool")
public class MatchmakerQueueMapPool extends AbstractEntity {

    private Double minRating;
    private Double maxRating;

    @Relationship("matchmakerQueue")
    private MatchmakerQueue matchmakerQueue;

    @Relationship("mapPool")
    @JsonIgnore
    private MapPool mapPool;

}
