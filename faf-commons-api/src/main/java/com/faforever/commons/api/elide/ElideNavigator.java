package com.faforever.commons.api.elide;

import com.faforever.commons.api.update.UpdateDto;
import com.github.jasminb.jsonapi.annotations.Type;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

/***
 * A utility class to build JSON-API / Elide-compatible URLs with include, filtering and sorting
 * @param <T>
 */
@Slf4j
public class ElideNavigator<T extends ElideEntity> implements ElideNavigatorSelector<T>, ElideNavigatorOnId<T>, ElideNavigatorOnCollection<T> {
  @Getter
  private final Class<T> dtoClass;
  private final Set<String> includes = new LinkedHashSet<>();
  private final Set<String> sorts = new LinkedHashSet<>();
  private final Optional<ElideNavigator<?>> parentNavigator;
  private Optional<String> id = Optional.empty();
  private Optional<String> relationship = Optional.empty();
  private Optional<Condition<?>> filterCondition = Optional.empty();
  private Optional<Integer> pageSize = Optional.empty();
  private Optional<Integer> pageNumber = Optional.empty();
  private Optional<Boolean> pageTotals = Optional.empty();

  private ElideNavigator(@NotNull Class<T> dtoClass) {
    this.dtoClass = dtoClass;
    this.parentNavigator = Optional.empty();
  }

  private ElideNavigator(@NotNull Class<T> dtoClass, @NotNull ElideNavigator<?> parentNavigator) {
    this.dtoClass = dtoClass;
    this.parentNavigator = Optional.of(parentNavigator);
  }

  public static <T extends QBuilder<T>> QBuilder<T> qBuilder() {
    return new QBuilder<>();
  }

  /**
   * Start a navigator for given type
   */
  public static <T extends ElideEntity> ElideNavigatorSelector<T> of(@NotNull Class<T> dtoClass) {
    return new ElideNavigator<>(dtoClass);
  }

  /**
   * Build a ElideNavigator directed to the given entity
   */
  public static <T extends ElideEntity> ElideNavigatorOnId<T> of(@NotNull T entity) {
    //noinspection unchecked
    return new ElideNavigator<>((Class<T>) entity.getClass()).id(entity.getId());
  }

  /**
   * Build a ElideNavigator directed to the given entity
   */
  public static <T extends ElideEntity> ElideNavigatorOnId<T> of(@NotNull UpdateDto<T> entity) {
    //noinspection unchecked
    return new ElideNavigator<>((Class<T>) entity.getClass()).id(entity.getId());
  }

  /**
   * Point to a certain id of entity type T
   */
  @Override
  public ElideNavigatorOnId<T> id(@NotNull String id) {
    this.id = Optional.of(id);
    return this;
  }

  /**
   * Point to a collection of type T
   */
  @Override
  public ElideNavigatorOnCollection<T> collection() {
    return this;
  }

  /**
   * Add an include to an ElideNavigator
   */
  @Override
  public ElideNavigator<T> addInclude(@NotNull String include) {
    log.trace("include added: {}", include);
    includes.add(include);
    return this;
  }

  @Override
  public <R extends ElideEntity> ElideNavigatorSelector<R> navigateRelationship(@NotNull Class<R> dtoClass, @NotNull String name) {
    if (!includes.isEmpty()) {
      throw new IllegalStateException("Cannot navigate relationship with includes on parent");
    }
    log.trace("relationship added: {}", name);
    this.relationship = Optional.of(name);
    return new ElideNavigator<>(dtoClass, this);
  }

  /**
   * Add a sorting rule to the navigator
   * Important: You need to give the full qualified route, there is NO referencing of parent relationships.
   * This is due to the fact that you need full control over the order of the sorting.
   */
  @Override
  public ElideNavigatorOnCollection<T> addSortingRule(@NotNull String field, boolean ascending) {
    log.trace("{} sort added: {}", ascending ? "ascending" : "descending", field);
    sorts.add((ascending ? "" : "-") + field);
    return this;
  }

  /**
   * Add a filter to a collection-pointed ElideNavigator
   */
  @Override
  public ElideNavigatorOnCollection<T> setFilter(@NotNull Condition<?> eq) {
    log.trace("filter set: {}", eq.query(new RSQLVisitor()));
    filterCondition = Optional.of(eq);
    return this;
  }

  @Override
  public Optional<Condition<?>> getFilter() {
    return filterCondition;
  }

  @Override
  public ElideNavigatorOnCollection<T> pageSize(int size) {
    log.trace("page size set: {}", size);
    pageSize = Optional.of(size);
    return this;
  }

  @Override
  public ElideNavigatorOnCollection<T> pageNumber(int number) {
    log.trace("page number set: {}", number);
    pageNumber = Optional.of(number);
    return this;
  }

  @Override
  public ElideNavigatorOnCollection<T> pageTotals(boolean showTotals) {
    log.trace("page totals set: {}", showTotals);
    pageTotals = Optional.of(showTotals);
    return this;
  }

  @Override
  public String build() {
    String dtoPath = dtoClass.getDeclaredAnnotation(Type.class).value();

    StringJoiner queryArgs = new StringJoiner("&", "?", "").setEmptyValue("");

    if (includes.size() > 0) {
      queryArgs.add(String.format("include=%s", String.join(",", includes)));
    }

    filterCondition.ifPresent(cond -> queryArgs.add(String.format("filter=%s", cond.query(new RSQLVisitor()))));

    if (sorts.size() > 0) {
      queryArgs.add(String.format("sort=%s", String.join(",", sorts)));
    }

    pageSize.ifPresent(i -> queryArgs.add(String.format("page[size]=%s", i)));
    pageNumber.ifPresent(i -> queryArgs.add(String.format("page[number]=%s", i)));
    pageTotals.ifPresent(show -> {
      if (show) {
        queryArgs.add("page[totals]");
      }
    });

    String route = parentNavigator.map(ElideNavigator::build).orElse("/data/" + dtoPath) +
                   id.map(i -> "/" + i).orElse("") +
                   relationship.map(r -> "/" + r).orElse("") +
                   queryArgs;
    log.trace("Route built: {}", route);
    return route;
  }
}
