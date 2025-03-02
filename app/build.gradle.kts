plugins {
  kotlin("multiplatform")
}

kotlin {
  jvm()
  jvmToolchain(11)
  linuxX64 {
    binaries {
      executable {
        entryPoint = "org.jraf.wstominitel.main"
        baseName = "ws-to-minitel"
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        // Argument parsing
        implementation(KotlinX.cli)

        // Date/time
        implementation(KotlinX.datetime)

        // IO
        implementation("org.jetbrains.kotlinx:kotlinx-io-core:_")

        // Minitel
        implementation("org.jraf:klibminitel:_")

        // Ktor client
        implementation(Ktor.client.core)
        implementation(Ktor.client.logging)
        implementation("io.ktor:ktor-client-websockets:_")
      }
    }

    val jvmMain by getting {
      dependencies {
        // Ktor client
        implementation(Ktor.client.okHttp)

        // Disable slf4j warning
        implementation("org.slf4j:slf4j-nop:_")
      }
    }

    val linuxX64Main by getting {
      dependencies {
        // Ktor client
        // Note: on Linux we need libcurl installed, e.g.:
        // sudo apt-get install libcurl4-gnutls-dev
        // See https://ktor.io/docs/client-engines.html#curl
        implementation(Ktor.client.curl)
      }
    }
  }
}
