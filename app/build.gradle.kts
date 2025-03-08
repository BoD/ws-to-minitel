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
  linuxArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.wstominitel.main"
        baseName = "ws-to-minitel"
      }
    }
  }
  macosArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.wstominitel.main"
        baseName = "ws-to-minitel"
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // Argument parsing
        implementation("com.github.ajalt.clikt:clikt:_")

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

    jvmMain {
      dependencies {
        // Ktor client
        implementation(Ktor.client.okHttp)

        // Disable slf4j warning
        implementation("org.slf4j:slf4j-nop:_")
      }
    }

    nativeMain {
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
