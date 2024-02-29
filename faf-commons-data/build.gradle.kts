version = "1.0-SNAPSHOT"

dependencies {
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  compileOnly(libs.jetbrains.annotations)

  implementation(libs.luaj.jse)
  implementation(libs.guava)
  implementation(libs.jackson.databind)
  implementation(libs.zstd.jni)
  api(libs.commons.compress)

  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testCompileOnly(libs.jetbrains.annotations)
  testImplementation(libs.logback.classic)
}
