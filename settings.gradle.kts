pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"

  // See https://jmfayard.github.io/refreshVersions
  id("de.fayard.refreshVersions") version "0.60.6"
}

rootProject.name = "ws-to-minitel"

include(
  ":app",
  ":main-jvm",
)
