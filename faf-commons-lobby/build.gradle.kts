import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.spring)
}

version = "1.0.0-SNAPSHOT"

dependencies {
  api(libs.reactor.core)
  implementation(libs.reactor.netty)
  compileOnly(libs.slf4j.api)
  compileOnly(libs.jackson.databind)

  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.junit.jupiter)
  testImplementation(libs.reactor.test)
  testImplementation(libs.logback.classic)
  testImplementation(libs.slf4j.api)
  testImplementation(libs.jackson.core)
  testImplementation(libs.jackson.module.kotlin)
  testImplementation(libs.jackson.datatype.jsr310)
  testImplementation(libs.jsonassert)
}

tasks.withType<Test> {
  useJUnitPlatform()

  testLogging {
    events(FAILED, STANDARD_ERROR, STANDARD_OUT, STANDARD_ERROR)
    exceptionFormat = FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
  }

  systemProperties["junit.jupiter.execution.parallel.enabled"] = true
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "21"
  }
}
