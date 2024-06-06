package com.faforever.commons.replay.header;

import java.util.Map;

/**
 * @param mapScenarioPath "/maps/scmp_026/scmp_026_scenario.lua" , as defined in the game options
 * @param mapBinaryPath "/maps/SCMP_026/SCMP_026.scmap" , as defined by the `map` field in `_scenario.lua`
 * @param mapVersion Can be null for maps with no versioning (gpg maps)
 * @param mapDescription Description of the map
 * @param mapScriptPath "/maps/SCMP_026/SCMP_026_script.lua", as defined by the `script` field in `_scenario.lua`
 * @param mapSavePath "/maps/SCMP_026/SCMP_026_save.lua", as defined by the `save` field in `_scenario.lua`
 * @param mapName Name of the map
 * @param mapSizeX Width of the (height)map, does not take into account reductions of playable area via map script
 * @param mapSizeZ Height of the (height)map, does not take into account reductions of playable area via map script
 * @param reclaimMassValue Total initial mass reclaim value of props, does not take into account wrecks
 * @param reclaimEnergyValue Total initial energy reclaim value of props
 * @param gameOptions Standard game options
 * @param modOptions Options that originate from mods
 */
public record GameScenario(
  String mapScenarioPath,
  String mapBinaryPath,
  Integer mapVersion,
  String mapDescription,
  String mapScriptPath, String mapSavePath,
  String mapName, Integer mapSizeX, Integer mapSizeZ, Integer reclaimMassValue,
  Integer reclaimEnergyValue, GameOptions gameOptions,
  Map<String, String> modOptions) {
}
