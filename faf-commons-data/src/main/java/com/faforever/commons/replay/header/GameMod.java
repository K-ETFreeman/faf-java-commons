package com.faforever.commons.replay.header;

/**
 * Populated by the game mods field of the table that is passed to `CLobby:LaunchGame`
 *
 * @param location
 * @param icon
 * @param copyright
 * @param name
 * @param description
 * @param author
 * @param uid
 * @param version
 * @param url
 */
public record GameMod(String location, String icon, String copyright, String name, String description, String author,
                      String uid, Integer version, String url) {
}
