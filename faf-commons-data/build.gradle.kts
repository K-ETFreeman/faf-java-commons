version = "1.0-SNAPSHOT"

dependencies {
  implementation(libs.luaj.jse)
  implementation(libs.guava)
  implementation(libs.jackson.databind)
  implementation(libs.zstd.jni)
  api(libs.commons.compress)

  testImplementation(libs.logback.classic)
}
