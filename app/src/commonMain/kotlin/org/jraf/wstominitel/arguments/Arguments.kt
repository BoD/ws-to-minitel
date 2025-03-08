/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2025-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.wstominitel.arguments

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum

class Arguments : CliktCommand("ws-to-minitel") {
  init {
    context {
      helpFormatter = { MordantHelpFormatter(it, showDefaultValues = true) }
    }
  }

  val url: String by option(
    "-u",
    "--url",
    metavar = "url",
    help = "WebSocket URL to connect to, e.g. wss://example.com:8080/ws",
  )
    .required()

  class SeparateInputOutputOptions : OptionGroup() {
    val input: String by option(
      "-i",
      "--input",
      metavar = "file",
      help = "File where to read from the keyboard, e.g. /dev/ttyUSB0. Either --input-output or both --input and --output must be provided.",
    )
      .required()
    val output: String by option(
      "-o",
      "--output",
      metavar = "file",
      help = "File where to write to the screen, e.g. /dev/ttyUSB0. Either --input-output or both --input and --output must be provided.",
    )
      .required()
  }

  val inputOutput: String? by option(
    "--input-output",
    "-io",
    metavar = "file",
    help = "File where to read from the keyboard and write to the screen, e.g. /dev/ttyUSB0. Either --input-output or both --input and --output must be provided.",
  )

  val separateInputOutput: SeparateInputOutputOptions? by SeparateInputOutputOptions().cooccurring()

  enum class LocalEcho {
    ON,
    OFF,
    NEITHER,
  }

  val localEcho: LocalEcho by option(
    "-e",
    "--local-echo",
    help = "Send a local echo on or off command first. By default, local echo off is sent. Use 'neither' to not send any command.",
  )
    .enum<LocalEcho> { it.name.lowercase() }
    .default(LocalEcho.OFF)

  val saveFramesToFiles: String? by option(
    "-s",
    "--save-frames",
    help = "Save received frames to files in the specified directory. Files will be named frame-<frame number>.vdt.",
  )

  enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    NONE,
  }

  val logLevel: LogLevel by option(
    "-l",
    "--log-level",
    help = "Set the log level",
  )
    .enum<LogLevel> { it.name.lowercase() }
    .default(LogLevel.INFO)

  override fun run() = Unit
}
