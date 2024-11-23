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

  configure<PublishingExtension> {
    publications {
      create<MavenPublication>("maven") {
        from(components["java"])
        pom {
          name.set(project.properties["project_display_name"].toString())
          description.set(project.properties["project_description"].toString())
          url.set(project.properties["project_website"].toString())
          issueManagement {
            system.set("GitHub")
            url.set(project.properties["project_issues"].toString())
          }
          scm {
            url.set(project.properties["project_website"].toString())
            connection.set("scm:git:${project.properties["project_vcs"].toString()}")
            developerConnection.set("scm:git:${project.properties["project_vcs_git"].toString()}")
          }
          licenses {
            license {
              name.set("The MIT License (MIT)")
              url.set("http://www.opensource.org/licenses/mit-license.php")
              distribution.set("repo")
            }
          }
          developers {
            developer {
              id.set("Brutus5000")
              name.set("Brutus5000")
              organization {
                name.set("FAForever")
                url.set("https://github.com/FAForever")
              }
            }
          }
        }
      }
    }

    repositories {
      maven {
        name = "MavenCentral"
        url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        credentials {
          username = project.properties["sonatypeUsername"].toString()
          password = project.properties["sonatypePassword"].toString()
        }
      }
    }
  }
}
