package com.faforever.commons.api.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum LinkedServiceType {
  STEAM,
  GOG,
  DISCORD,
  PATREON,
  @JsonEnumDefaultValue UNKNOWN
}
