package com.faforever.commons.mod;

import lombok.Data;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.util.ArrayList;
import java.util.List;

@Data
public class Mod {

  private String name;
  private String uid;
  private ComparableVersion version;
  private String copyright;
  private String icon;
  private String description;
  private String author;

  /** Used to reference the forums or Github-like URLs */
  private String url;
  private boolean selectable;
  private boolean uiOnly;

  /** Used by featured mods */
  private final List<MountInfo> mountInfos = new ArrayList<>();

  /** Used by featured mods */
  private final List<String> hookDirectories = new ArrayList<>();
}
