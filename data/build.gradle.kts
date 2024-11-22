java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  compileOnly(libs.jetbrains.annotations)
  compileOnly(libs.slf4j.api)

  implementation(libs.luaj.jse)
  implementation(libs.guava)
  implementation(libs.jackson.databind)
  implementation(libs.zstd.jni)
  api(libs.commons.compress)

  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.hamcrest.core)
  testImplementation(libs.mockito.core)
  testImplementation(libs.mockito.junit.jupiter)
  testCompileOnly(libs.jetbrains.annotations)
  testImplementation(libs.logback.classic)
}
