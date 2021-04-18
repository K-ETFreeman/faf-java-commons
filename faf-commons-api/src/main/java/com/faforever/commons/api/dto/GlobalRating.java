package com.faforever.commons.api.dto;

import com.github.jasminb.jsonapi.annotations.Type;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @deprecated GlobalRating replaced with leaderboardRating
 */
@Deprecated
@ToString(onlyExplicitlyIncluded = true, callSuper = true)
@Type("globalRating")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class GlobalRating extends Rating {
}
