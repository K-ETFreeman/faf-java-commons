package com.faforever.commons.api.dto;

import lombok.Data;

@Data
public class NeroxisGeneratorParams implements MapParams {
  private int spawns;
  private int size;
  private String version;
}
