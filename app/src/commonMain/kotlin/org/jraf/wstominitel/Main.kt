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

package org.jraf.wstominitel

import com.github.ajalt.clikt.core.main
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import org.jraf.klibminitel.core.Minitel
import org.jraf.wstominitel.arguments.Arguments
import org.jraf.wstominitel.http.createHttpClient
import org.jraf.wstominitel.util.LogLevel
import org.jraf.wstominitel.util.logd
import org.jraf.wstominitel.util.loge
import org.jraf.wstominitel.util.logi
import org.jraf.wstominitel.util.logw
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private class WsToMinitel(
  private val arguments: Arguments,
) {
  private var frameNumber = 0

  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  suspend fun run() {
    val logLevel = arguments.logLevel
    initLogs(logLevel)

    logi("BoD ws-to-minitel v1.0.0")

    if (arguments.inputOutput == null && arguments.separateInputOutput == null ||
      arguments.inputOutput != null && arguments.separateInputOutput != null
    ) {
      loge("Either --input-output or both --input and --output must be provided, exiting")
      return
    }

    val minitel = Minitel(
      keyboardFilePath = arguments.separateInputOutput?.input ?: arguments.inputOutput!!,
      screenFilePath = arguments.separateInputOutput?.output ?: arguments.inputOutput!!,
    )
    minitel.connect {
      var webSocketJob = onMinitelTurnedOn()

      system.collect { systemEvent ->
        if (systemEvent is Minitel.SystemEvent.TurnedOnEvent) {
          logd("Turned on: restarting WebSocket client")
          webSocketJob.cancel()
          webSocketJob = onMinitelTurnedOn()
        }
      }
    }
  }

  private suspend fun Minitel.Connection.onMinitelTurnedOn(): Job {
    disableAcknowledgement()
    disableEnableLocalEcho(arguments.localEcho)
    screen.showCursor(false)
    screen.clearScreenAndHome()
    return coroutineScope.launch { keepWebSocketClientConnected(this@onMinitelTurnedOn) }
  }

  private fun initLogs(logLevel: Arguments.LogLevel) {
    org.jraf.wstominitel.util.logLevel = when (logLevel) {
      Arguments.LogLevel.DEBUG -> LogLevel.DEBUG
      Arguments.LogLevel.INFO -> LogLevel.INFO
      Arguments.LogLevel.WARN -> LogLevel.WARNING
      Arguments.LogLevel.NONE -> LogLevel.NONE
    }
  }

  private suspend fun Minitel.Connection.disableAcknowledgement() {
    logd("Sending disable acknowledgement command")
    screen.disableAcknowledgement()
  }

  private suspend fun Minitel.Connection.disableEnableLocalEcho(echo: Arguments.LocalEcho) {
    when (echo) {
      Arguments.LocalEcho.ON -> {
        logd("Sending local echo ON command")
        screen.localEcho(true)
      }

      Arguments.LocalEcho.OFF -> {
        logd("Sending local echo OFF command")
        screen.localEcho(false)
      }

      Arguments.LocalEcho.NEITHER -> {}
    }
  }

  private suspend fun keepWebSocketClientConnected(connection: Minitel.Connection, times: Int = 10) {
    repeat(times) {
      try {
        startWebSocketClient(connection)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        logw(e, "WebSocket client failed")
      }
      logd("Retrying WebSocket in 10 seconds")
      delay(10.seconds)
    }
    logw("WebSocket client failed $times times, sleeping forever")
    delay(Duration.INFINITE)
  }

  private suspend fun startWebSocketClient(connection: Minitel.Connection) {
    createHttpClient().use {
      it.webSocket(arguments.url) {
        launch {
          connection.keyboard.collect {
            val readBytes = it.raw()
            logd("Sending: ${readBytes.size} keyboard bytes")
            outgoing.send(Frame.Text(true, readBytes))
          }
        }

        while (true) {
          when (val frame = incoming.receive()) {
            is Frame.Text, is Frame.Binary -> {
              val data = frame.data
              logd("Received ${frame::class.simpleName} frame (${data.size} bytes)")
              saveFrame(data)
              connection.screen.raw(data)
            }

            is Frame.Close -> {
              logd("Received Close frame: exiting")
              break
            }

            else -> {
              logd("Received: $frame")
            }
          }
        }
      }
    }
  }

  private fun saveFrame(readBytes: ByteArray) {
    val saveFramesToFiles = arguments.saveFramesToFiles
    if (saveFramesToFiles == null) return
    val frameNumberPadded = frameNumber.toString().padStart(3, '0')

    val dir = Path(saveFramesToFiles)
    SystemFileSystem.createDirectories(dir)
    val file = Path(saveFramesToFiles, "frame-$frameNumberPadded.vdt")
    logi("Saving frame to $file")
    SystemFileSystem.sink(file).buffered().use {
      it.write(readBytes)
      it.flush()
    }
    frameNumber++
  }
}

fun main(av: Array<String>) {
  val arguments = Arguments()
  arguments.main(av)

  runBlocking {
    WsToMinitel(arguments).run()
  }
}
