package com.faforever.commons.api.elide;

public interface ElideNavigatorOnId<T extends ElideEntity> {

  ElideNavigatorOnId<T> addInclude(String include);

  <R extends ElideEntity> ElideNavigatorSelector<R> navigateRelationship(Class<R> entityClass, String name);

  Class<T> getDtoClass();

  String build();
}
