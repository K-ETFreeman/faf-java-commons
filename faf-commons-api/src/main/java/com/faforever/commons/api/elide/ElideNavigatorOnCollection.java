package com.faforever.commons.api.elide;

import com.github.rutledgepaulv.qbuilders.conditions.Condition;

import java.util.Optional;

public interface ElideNavigatorOnCollection<T extends ElideEntity> extends ElideEndpointBuilder<T>{

  ElideNavigatorOnCollection<T> addInclude(String include);

  ElideNavigatorOnCollection<T> addSortingRule(String field, boolean ascending);

  Optional<Condition<?>> getFilter();

  ElideNavigatorOnCollection<T> setFilter(Condition<?> eq);

  ElideNavigatorOnCollection<T> pageSize(int size);

  ElideNavigatorOnCollection<T> pageNumber(int number);

  ElideNavigatorOnCollection<T> pageTotals(boolean showTotals);
}
