/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2021-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jraf.wstominitel.arguments

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required

class Arguments(av: Array<String>) {
  private val parser = ArgParser("ws-to-minitel")

  val url: String by parser.option(
    type = ArgType.String,
    shortName = "u",
    description = "WebSocket URL to connect to, e.g. wss://example.com/ws:8080"
  )
    .required()

  val input: String by parser.option(
    type = ArgType.String,
    shortName = "i",
    description = "Input where to read the keyboard, e.g. /dev/ttyUSB0",
  )
    .required()

  val output: String by parser.option(
    type = ArgType.String,
    shortName = "o",
    description = "Output where to write the screen, e.g. /dev/ttyUSB1",
  )
    .required()

  enum class LocalEcho {
    ON,
    OFF,
  }

  val localEcho: LocalEcho? by parser.option(
    type = ArgType.Choice<LocalEcho>(),
    fullName = "local-echo",
    shortName = "e",
    description = "Send an local echo on or off command first. By default, no command is sent."
  )

  val saveFramesToFiles: String? by parser.option(
    type = ArgType.String,
    fullName = "save-frames",
    shortName = "s",
    description = "Save received frames to files in the specified directory. Files will be named frame-<frame number>.vdt.",
  )

  enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    NONE,
  }

  val logLevel: LogLevel by parser.option(
    type = ArgType.Choice<LogLevel>(),
    fullName = "log-level",
    shortName = "l",
    description = "Set the log level",
  ).default(LogLevel.INFO)

  init {
    parser.parse(av)
  }
}
