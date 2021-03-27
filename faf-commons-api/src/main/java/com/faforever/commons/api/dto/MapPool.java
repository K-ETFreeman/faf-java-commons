package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Relationship;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Type("mapPool")
public class MapPool extends AbstractEntity {

    private String name;

    @Relationship("matchmakerQueueMapPool")
    @JsonIgnore
    private MatchmakerQueueMapPool matchmakerQueueMapPool;

    @Relationship("mapPoolAssignments")
    private List<MapPoolAssignment> mapPoolAssignments;

}
