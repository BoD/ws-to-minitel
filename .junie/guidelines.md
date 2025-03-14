# ws-to-minitel Developer Guidelines

This document provides concise guidance for new developers working on the ws-to-minitel project.

## Project Overview

ws-to-minitel is a Kotlin Multiplatform tool that bridges WebSocket servers and Minitel terminals. It allows connecting vintage Minitel
terminals to modern web services.

## Tech Stack

- **Language**: Kotlin Multiplatform
- **Build System**: Gradle with Kotlin DSL
- **Dependencies**:
  - Clikt: Command-line argument parsing
  - Ktor: WebSocket client
  - KotlinX IO: File operations
  - KlibMinitel: Minitel communication library

## Project Structure

```
ws-to-minitel/
├── app/                    # Main application module (multiplatform)
│   ├── src/
│   │   ├── commonMain/     # Shared code for all platforms
│   │   ├── jvmMain/        # JVM-specific code
│   │   └── nativeMain/     # Native platform code
├── main-jvm/               # JVM-specific entry point
├── scripts/                # Helper scripts
│   ├── go.sh              # Main execution script
│   ├── init_tty.sh        # TTY initialization script
│   └── *.sh               # Service-specific scripts
```

## Building the Project

```bash
# Update dependencies
./gradlew refreshVersions

# Build all executables
./gradlew build

# Build specific platform
./gradlew :app:linkReleaseExecutableLinuxX64  # Linux x64
./gradlew :app:linkReleaseExecutableLinuxArm64  # Linux ARM64
./gradlew :app:linkReleaseExecutableMacosArm64  # macOS ARM64
./gradlew :main-jvm:build  # JVM
```

## Running the Application

### Using Gradle

```bash
./gradlew :main-jvm:run --args="--url ws://example.com:8080 --input-output /dev/ttyUSB0"
```

### Using Scripts

```bash
# Initialize TTY (if needed)
sudo ./scripts/init_tty.sh

# Connect to a specific service
./scripts/minipavi.sh  # Connect to MiniPAVI
./scripts/jraf.sh      # Connect to JRAF

# Connect to a custom service
./scripts/go.sh ws://example.com:8080
```

## Code Organization

- **Main Application Logic**: `app/src/commonMain/kotlin/org/jraf/wstominitel/Main.kt`
- **Command-line Arguments**: `app/src/commonMain/kotlin/org/jraf/wstominitel/arguments/`
- **HTTP Client**: `app/src/commonMain/kotlin/org/jraf/wstominitel/http/`
- **Utilities**: `app/src/commonMain/kotlin/org/jraf/wstominitel/util/`

## Best Practices

1. **Code Style**:
  - Follow Kotlin coding conventions
  - Use meaningful variable and function names
  - Include license header in source files

2. **Error Handling**:
  - Use proper exception handling
  - Implement retry mechanisms for network operations
  - Log errors with appropriate levels

3. **Logging**:
  - Use appropriate log levels (DEBUG, INFO, WARN, ERROR)
  - Include context in log messages

4. **Resource Management**:
  - Use `use` blocks for closing resources
  - Properly handle coroutine cancellation

5. **Platform-specific Code**:
  - Keep platform-specific code in appropriate source sets
  - Use expect/actual for platform-specific implementations

## Adding New Features

1. Identify which source set the code belongs to (common, JVM, native)
2. Implement the feature with proper error handling and logging
3. Update command-line arguments if needed
4. Test on all supported platforms
5. Add helper scripts if appropriate

## Troubleshooting

- **Serial Port Issues**: Run `scripts/init_tty.sh` to initialize the TTY
- **Build Failures**: Check Gradle version and plugin compatibility
- **WebSocket Connection Issues**: Verify URL and network connectivity
