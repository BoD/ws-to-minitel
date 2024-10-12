plugins {
  kotlin("jvm")
  application
}

group = "org.jraf"
version = "1.0.0"

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  // Slf4j
  implementation("org.slf4j:slf4j-api:_")
  implementation("org.slf4j:slf4j-simple:_")

  // Ktor client
  implementation(Ktor.client.core)
  implementation(Ktor.client.logging)
  implementation(Ktor.client.okHttp)
  implementation("io.ktor:ktor-client-websockets:_")

  // Argument parsing
  implementation(KotlinX.cli)

  // Tests
  testImplementation(Kotlin.test)
}

application {
  mainClass.set("org.jraf.wstominitel.MainKt")
}

tasks.test {
  useJUnitPlatform()
}

// `./gradlew refreshVersions` to update dependencies
