package com.faforever.commons.api.dto;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class TutorialTest {


  @Test
  void testTutorialToString() {
    Tutorial tutorial = new Tutorial();
    Tutorial secondTutorial = new Tutorial();
    TutorialCategory category = new TutorialCategory();
    tutorial.setCategory(category);
    category.setTutorials(Arrays.asList(tutorial, secondTutorial));
    assertThat("correct to string", tutorial.toString(), is(notNullValue()));
  }

  @Test
  void testTutorialCategoryToString() {
    TutorialCategory tutorialCategory = getTutorialCategory();
    assertThat("correct to string", tutorialCategory.toString(),
      is(String.format("TutorialCategory(id=%s, categoryKey=%s, category=%s)", tutorialCategory.getId(),
        tutorialCategory.getCategoryKey(), tutorialCategory.getCategory())));
  }

  @Test
  void testTutorialEquals() {
    Tutorial tutorial = new Tutorial();
    Tutorial secondTutorial = new Tutorial();
    assertThat("tutorial equals", tutorial.equals(secondTutorial), is(true));
  }

  @Test
  void testTutorialHashCode() {
    Tutorial tutorial = new Tutorial();
    Tutorial secondTutorial = new Tutorial();
    assertThat("tutorial equals", tutorial.hashCode(), is(secondTutorial.hashCode()));
  }

  @Test
  void testTutorialCategoryEquals() {
    TutorialCategory tutorialCategory = getTutorialCategory();
    TutorialCategory secondTutorialCategory = getTutorialCategory();
    assertThat("tutorial equals", tutorialCategory.equals(secondTutorialCategory), is(true));
  }

  @Test
  void testTutorialCategoryHashCode() {
    TutorialCategory tutorialCategory = getTutorialCategory();
    TutorialCategory secondTutorialCategory = getTutorialCategory();
    assertThat("tutorial equals", tutorialCategory.hashCode(), is(secondTutorialCategory.hashCode()));
  }

  @NotNull
  private TutorialCategory getTutorialCategory() {
    TutorialCategory tutorialCategory = new TutorialCategory();
    tutorialCategory.setId("fixedId");
    tutorialCategory.setCategory("fixedCategory");
    tutorialCategory.setCategoryKey("fixedCategoryKey");

    Tutorial tutorial = new Tutorial();
    tutorial.setCategory(tutorialCategory);
    tutorialCategory.setTutorials(Collections.singletonList(tutorial));
    return tutorialCategory;
  }

}
