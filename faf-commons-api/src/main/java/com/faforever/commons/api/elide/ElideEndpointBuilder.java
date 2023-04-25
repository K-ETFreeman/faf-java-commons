package com.faforever.commons.api.elide;

public interface ElideEndpointBuilder<T extends ElideEntity> {
  String build();

  Class<T> getDtoClass();

  ElideEndpointBuilder<T> addInclude(String include);

  boolean isRoot();
}
