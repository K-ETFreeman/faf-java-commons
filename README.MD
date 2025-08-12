# faf-java-commons

A library of shared code for the FAForever java projects:
* [FAF API](https://github.com/FAForever/faf-java-api)
* [FAF client](https://github.com/FAForever/downlords-faf-client)
* [FAF moderator client](https://github.com/FAForever/faf-moderator-client)

## Modules

This repository contains 3 standalone modules:

* **API** contains code to interact with the FAF API
* **Lobby** contains code to interact with the FAF Lobby server.
* **Data** contains code to interact with FAF and SupCom file formats in general.

## Versioning and compatibility

This project does **not** follow semantic versioning. The published packages follow a `<YYMMDD>-<git commit hash>`
pattern to always suggest the latest version.

There might be breaking changes. We always enforce compatibility to the most recent API.

This project does **not** ensure backwards compatibility for 3rd party applications. Use at your own
risk.


## Releasing

The project is released on [maven central](https://mvnrepository.com/artifact/com.faforever.commons).

It also can be released on Jitpack but with its special Jitpack group id. We no longer ensure a working Jitpack release.

**Maintainer info:**
To release a new version, trigger the `Publish package to MavenCentral` Github action. Afterwards it needs to be
published on [Sonatype](https://central.sonatype.com/publishing/namespaces) with the FAForever user. It always takes
some time to show up afterwards.
