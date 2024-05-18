package com.faforever.commons.replay.header;

/**
 * Populated by the game mods field of the table that is passed to `CLobby:LaunchGame`
 * @param location
 * @param name
 * @param description
 * @param author
 * @param uid
 * @param version
 * @param url
 * @param urlGithub
 */
public record GameMod(String location, String name, String description, String author, String uid, String version,
                      String url, String urlGithub) {
}
