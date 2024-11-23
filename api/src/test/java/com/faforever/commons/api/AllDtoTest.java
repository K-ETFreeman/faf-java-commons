package com.faforever.commons.api;

import com.faforever.commons.api.dto.AbstractEntity;
import com.faforever.commons.api.dto.Avatar;
import com.faforever.commons.api.dto.Player;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import pl.pojo.tester.api.PackageFilter;
import pl.pojo.tester.api.assertion.Assertions;
import pl.pojo.tester.internal.utils.ReflectionUtils;

import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AllDtoTest {

  @Test
  void allDto() {
    // TODO test getter and setter
    Assertions.assertPojoMethodsForAll(new DtoFilter()).quickly();
  }

  @Test
  void unequalityAbstractEntityTest() {
    // Reproduction of issue #11
    AbstractEntity<?> player = new Player().setId("equal-id");
    AbstractEntity<?> avatar = new Avatar().setId("equal-id");

    assertNotEquals(player, avatar);
  }

  private static class DtoFilter implements PackageFilter {

    @Override
    @SneakyThrows
    public Class<?>[] getClasses() {
      return Stream.of(ReflectionUtils.getClassesFromPackage(AbstractEntity.class.getPackage().getName()))
        .filter(aClass -> !aClass.isEnum() && !Modifier.isAbstract(aClass.getModifiers()))
        .toArray(Class[]::new);
    }
  }
}
