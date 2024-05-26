package com.faforever.commons.replay.header;

import java.util.Map;

public record GameScenario(String mapPath, Integer mapVersion, String mapDescription, String mapScript, String mapSave,
                           String mapName, Integer mapSizeX, Integer mapSizeZ, Integer reclaimMassValue,
                           Integer reclaimEnergyValue, GameOptions gameOptions,
                           Map<String, String> modOptions) {
}
