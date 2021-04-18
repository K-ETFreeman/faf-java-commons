package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @deprecated Ladder1v1Rating replaced with leaderboardRating
 */
@Deprecated
@Type("ladder1v1Rating")
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class Ladder1v1Rating extends Rating {
}
