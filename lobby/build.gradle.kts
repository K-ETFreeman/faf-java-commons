import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR
import org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_OUT
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.plugin.spring)
  alias(libs.plugins.com.vanniktech.maven.publish)
}

dependencies {
  api(libs.reactor.core)
  implementation(libs.reactor.netty)
  compileOnly(libs.slf4j.api)
  compileOnly(libs.jackson.databind)

  testImplementation(platform(libs.junit.bom))
  testRuntimeOnly(libs.junit.platform.launcher)
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

kotlin {
  compilerOptions {
    freeCompilerArgs.set(listOf("-Xjsr305=strict"))
    jvmTarget.set(JvmTarget.JVM_21)
  }
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()

  coordinates("com.faforever.commons", "lobby", project.version.toString())
  // Configure POM metadata
  pom {
    name.set("lobby")
    description.set("Lobby client implementation for FAForever")
    url.set("https://github.com/FAForever/faf-java-commons")
    licenses {
      license {
        name.set("MIT")
        url.set("https://www.opensource.org/licenses/mit-license.php")
      }
    }
    developers {
      developer {
        id.set("Brutus5000")
        name.set("Brutus5000")
        organization.set("FAForever")
        organizationUrl.set("https://github.com/FAForever")
      }
    }
    scm {
      url.set("https://github.com/FAForever/faf-java-commons")
      connection.set("scm:git:https://github.com/FAForever/faf-java-commons")
      developerConnection.set("scm:git:https://github.com/FAForever/faf-java-commons")
    }
    issueManagement {
      system.set("GitHub")
      url.set("https://github.com/FAForever/faf-java-commons/issues")
    }
  }
}
