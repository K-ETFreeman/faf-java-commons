package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum VictoryCondition {
  // Order is crucial
  DEMORALIZATION,
  DOMINATION,
  ERADICATION,
  SANDBOX,
  DECAPITATION,
  @JsonEnumDefaultValue
  UNKNOWN;
}
