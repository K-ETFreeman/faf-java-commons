package com.faforever.commons.replay.header;

import java.util.List;

/**
 * Populated by the table that is passed to `CLobby:LaunchGame`
 */
public record ReplayHeader(String gameVersion, String replayVersion, String pathToScenario, boolean cheatsEnabled, int seed,
                           List<Source> sources,
                           List<GameMod> mods,
                           GameScenario scenario,
                           List<PlayerOptions> playerOptions
) {
}
