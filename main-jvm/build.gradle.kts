plugins {
  kotlin("jvm")
  application
}

group = "org.jraf"
version = "1.0.0"

kotlin {
  jvmToolchain(11)
}

dependencies {
  implementation(project(":app"))
}

application {
  mainClass.set("org.jraf.wstominitel.MainKt")
  applicationName = "ws-to-minitel"
}
