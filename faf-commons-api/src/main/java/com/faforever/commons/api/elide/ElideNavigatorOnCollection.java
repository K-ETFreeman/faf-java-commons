package com.faforever.commons.api.elide;

import com.github.rutledgepaulv.qbuilders.conditions.Condition;

public interface ElideNavigatorOnCollection<T extends ElideEntity> {

  ElideNavigatorOnCollection<T> addInclude(String include);

  ElideNavigatorOnCollection<T> addSortingRule(String field, boolean ascending);

  ElideNavigatorOnCollection<T> addFilter(Condition<?> eq);

  ElideNavigatorOnCollection<T> pageSize(int size);

  ElideNavigatorOnCollection<T> pageNumber(int number);

  ElideNavigatorOnCollection<T> pageTotals(boolean showTotals);

  Class<T> getDtoClass();

  String build();
}
