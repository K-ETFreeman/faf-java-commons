plugins {
  alias(libs.plugins.com.vanniktech.maven.publish)
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(project(":data"))
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  compileOnly(libs.jetbrains.annotations)
  compileOnly(libs.slf4j.api)
  implementation(libs.jsonapi.converter)
  implementation(libs.q.builders)

  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testImplementation(platform(libs.junit.bom))
  testRuntimeOnly(libs.junit.platform.launcher)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.junit.jupiter)
  testImplementation(libs.pojo.tester)
  testCompileOnly(libs.jetbrains.annotations)
}

mavenPublishing {
  publishToMavenCentral()
  // signAllPublications() // Commented out for JitPack builds

  coordinates("com.faforever.commons", "api", project.version.toString())
  // Configure POM metadata
  pom {
    name.set("api")
    description.set("API DTOs for FAForever Java API")
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
