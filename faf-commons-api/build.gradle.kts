version = "1.0-SNAPSHOT"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(project(":faf-commons-data"))
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  compileOnly(libs.jetbrains.annotations)
  compileOnly(libs.slf4j.api)
  implementation(libs.jsonapi.converter)
  implementation(libs.q.builders)

  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.junit.jupiter)
  testImplementation(libs.pojo.tester)
  testCompileOnly(libs.jetbrains.annotations)
}
