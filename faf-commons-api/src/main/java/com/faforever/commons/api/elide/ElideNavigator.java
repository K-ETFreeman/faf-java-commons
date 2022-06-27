package com.faforever.commons.api.elide;

import com.faforever.commons.api.update.UpdateDto;
import com.github.jasminb.jsonapi.annotations.Type;
import com.github.rutledgepaulv.qbuilders.builders.QBuilder;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import com.github.rutledgepaulv.qbuilders.visitors.RSQLVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/***
 * A utility class to build JSON-API / Elide-compatible URLs with include, filtering and sorting
 * @param <T>
 */
@Slf4j
public class ElideNavigator<T extends ElideEntity> implements ElideNavigatorSelector<T>, ElideNavigatorOnId<T>, ElideNavigatorOnCollection<T> {
  @Getter
  private final Class<T> dtoClass;
  private final List<String> includes = new ArrayList<>();
  private final List<String> sorts = new ArrayList<>();
  private final Optional<ElideNavigator<?>> parentNavigator;
  private String parentName = null;
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

  private ElideNavigator(@NotNull Class<T> dtoClass, @NotNull ElideNavigator<?> parentNavigator, @NotNull String parentName) {
    this.dtoClass = dtoClass;
    this.parentNavigator = Optional.of(parentNavigator);
    this.parentName = parentName;
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
   *
   * @return
   */
  @Override
  public ElideNavigatorOnCollection<T> collection() {
    return this;
  }

  /**
   * Add an include to an ElideNavigator
   * Important: ElideNavigator takes care of referencing to the correct parent relationships. Just use a relative include.
   */
  @Override
  public ElideNavigator<T> addInclude(@NotNull String include) {
    log.trace("include added: {}", include);
    includes.add(include);
    return this;
  }

  @Override
  public <R extends ElideEntity> ElideNavigatorSelector<R> navigateRelationship(@NotNull Class<R> dtoClass, @NotNull String name) {
    log.trace("relationship added: {}", name);
    this.relationship = Optional.of(name);
    return new ElideNavigator<>(dtoClass, this, name);
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
   * Important: ElideNavigator takes care of referencing to the correct parent relationships. Just use a relative include.
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

  private String pathToRoot() {
    return parentNavigator.map(parent -> parent.pathToRoot() + parent.parentName + ".").orElse("");
  }

  @Override
  public String build() {
    String dtoPath = dtoClass.getDeclaredAnnotation(Type.class).value();

    StringJoiner queryArgs = new StringJoiner("&", "?", "")
      .setEmptyValue("");

    if (includes.size() > 0) {
      queryArgs.add(String.format("include=%s", includes.stream()
        .map(s -> pathToRoot() + s)
        .collect(Collectors.joining(","))));
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

    String route = parentNavigator.map(ElideNavigator::build)
      .orElse("/data/" + dtoPath) +
      id.map(i -> "/" + i).orElse("") +
      relationship.map(r -> "/" + r).orElse("") +
      queryArgs;
    log.trace("Route built: {}", route);
    return route;
  }
}
