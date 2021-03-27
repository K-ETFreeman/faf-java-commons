package com.faforever.commons.api.dto;

import lombok.Value;

@Value
public class NeroxisGeneratorParams implements MapParams {
  int spawns;
  int size;
  String version;
}
