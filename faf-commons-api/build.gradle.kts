version = "1.0-SNAPSHOT"

dependencies {
  implementation(project(":faf-commons-data"))
  implementation(libs.jsonapi.converter)
  implementation(libs.q.builders)

  testImplementation(libs.pojo.tester)
}
