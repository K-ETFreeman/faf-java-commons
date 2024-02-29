version = "1.0-SNAPSHOT"

dependencies {
  implementation(project(":faf-commons-data"))
  annotationProcessor(libs.lombok)
  compileOnly(libs.lombok)
  compileOnly(libs.jetbrains.annotations)
  implementation(libs.jsonapi.converter)
  implementation(libs.q.builders)

  testImplementation(libs.pojo.tester)
  testAnnotationProcessor(libs.lombok)
  testCompileOnly(libs.lombok)
  testCompileOnly(libs.jetbrains.annotations)
}
