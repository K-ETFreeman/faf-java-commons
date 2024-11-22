package com.faforever.commons.api.elide;

import com.faforever.commons.api.dto.Ladder1v1Map;
import com.faforever.commons.api.dto.MapPoolAssignment;
import com.faforever.commons.api.dto.MapVersion;
import com.github.rutledgepaulv.qbuilders.conditions.Condition;
import org.junit.jupiter.api.Test;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ElideNavigatorTest {
  @Test
  void testGetList() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class).collection().build(), is("/data/ladder1v1Map"));
  }

  @Test
  void testGetId() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class).id("5").build(), is("/data/ladder1v1Map/5"));
  }

  @Test
  void testGetListSingleInclude() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .collection()
                             .addInclude("mapVersion")
                             .build(), is("/data/ladder1v1Map?include=mapVersion"));
  }

  @Test
  void testGetListMultipleInclude() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .collection()
                             .addInclude("mapVersion")
                             .addInclude("mapVersion.map")
                             .build(), is("/data/ladder1v1Map?include=mapVersion,mapVersion.map"));
  }

  @Test
  void testGetListFiltered() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .collection()
                             .setFilter(qBuilder().intNum("mapVersion.id").gt(10).or().string("hello").eq("nana"))
                             .build(), is("/data/ladder1v1Map?filter=mapVersion.id=gt=\"10\",hello==\"nana\""));
  }

  @Test
  void testGetListCombinedFilter() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .collection()
                             .addInclude("mapVersion")
                             .addInclude("mapVersion.map")
                             .pageSize(10)
                             .pageNumber(3)
                             .setFilter(qBuilder().intNum("mapVersion.id").gt(10).or().string("hello").eq("nana"))
                             .build(), is("/data/ladder1v1Map?include=mapVersion,mapVersion.map&filter=mapVersion.id=gt=\"10\",hello==\"nana\"&page[size]=10&page[number]=3"));
  }

  @Test
  void testGetIdMultipleInclude() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .id("5")
                             .addInclude("mapVersion")
                             .addInclude("mapVersion.map")
                             .build(), is("/data/ladder1v1Map/5?include=mapVersion,mapVersion.map"));
  }

  @Test
  void testGetListSorted() {
    assertThat(ElideNavigator.of(MapPoolAssignment.class)
                             .collection()
                             .addSortingRule("sortCritASC", true)
                             .addSortingRule("sortCritDESC", false)
                             .build(), is("/data/mapPoolAssignment?sort=sortCritASC,-sortCritDESC"));
  }

  @Test
  void testNavigateFromIdToId() {
    assertThat(ElideNavigator.of(Ladder1v1Map.class)
                             .id("5")
                             .navigateRelationship(MapVersion.class, "mapVersion")
                             .id("1234")
                             .addInclude("author")
                             .build(), is("/data/ladder1v1Map/5/mapVersion/1234?include=author"));
  }

  @Test
  void testGetListPages() {
    assertThat(ElideNavigator.of(MapPoolAssignment.class)
                             .collection()
                             .pageSize(1)
                             .pageNumber(1)
                             .pageTotals(true)
                             .build(), is("/data/mapPoolAssignment?page[size]=1&page[number]=1&page[totals]"));
  }

  @Test
  void testGetFilter() {
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).collection();
    assertThat(navigator.getFilter().isEmpty(), is(true));
    Condition<?> condition = qBuilder().string("test").eq("test");
    navigator.setFilter(condition);
    assertThat(navigator.getFilter().get(), is(condition));
  }

  @Test
  void testCannotNavigateAfterIncludes() {
    assertThrows(IllegalStateException.class, () -> ElideNavigator.of(MapPoolAssignment.class)
                                                                  .id("1")
                                                                  .addInclude("mapVersion")
                                                                  .navigateRelationship(MapVersion.class, "mapVersion"));
  }

  @Test
  void testIsRootCollection() {
    ElideNavigatorOnCollection<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).collection();
    assertThat(navigator.isRoot(), is(true));
  }

  @Test
  void testIsRootId() {
    ElideNavigatorOnId<MapPoolAssignment> navigator = ElideNavigator.of(MapPoolAssignment.class).id("1");
    assertThat(navigator.isRoot(), is(true));
  }

  @Test
  void testIsNotRootCollection() {
    ElideNavigatorOnCollection<MapVersion> navigator = ElideNavigator.of(MapPoolAssignment.class)
                                                                     .id("1")
                                                                     .navigateRelationship(MapVersion.class, "mapVersion")
                                                                     .collection();
    assertThat(navigator.isRoot(), is(false));
  }

  @Test
  void testIsNotRootId() {
    ElideNavigatorOnId<MapVersion> navigator = ElideNavigator.of(MapPoolAssignment.class)
                                                             .id("1")
                                                             .navigateRelationship(MapVersion.class, "mapVersion")
                                                             .id("1");
    assertThat(navigator.isRoot(), is(false));
  }

}
