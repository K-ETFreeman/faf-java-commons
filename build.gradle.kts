plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.test.logger) apply false
}

configure(subprojects) {
  apply(plugin = "java-library")
  apply(plugin = "com.adarshr.test-logger")
  apply(plugin = "maven-publish")

  group = "com.faforever.commons"

  repositories {
    mavenCentral()
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}
