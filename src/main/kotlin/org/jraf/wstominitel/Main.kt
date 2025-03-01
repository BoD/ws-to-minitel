/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.jraf.klibminitel.core.Minitel
import org.jraf.wstominitel.arguments.Arguments
import org.jraf.wstominitel.util.insecureSocketFactory
import org.jraf.wstominitel.util.logd
import org.jraf.wstominitel.util.logi
import org.jraf.wstominitel.util.logw
import org.jraf.wstominitel.util.naiveTrustManager
import org.slf4j.simple.SimpleLogger
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private class WsToMinitel(
  private val arguments: Arguments,
) {
  private val minitel = Minitel(
    keyboard = (arguments.input?.let { FileInputStream(it) } ?: System.`in`).asSource().buffered(),
    screen = (arguments.output?.let { FileOutputStream(it) } ?: System.out).asSink().buffered(),
  )

  private var frameNumber = 0

  private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  suspend fun run() {
    val logLevel = arguments.logLevel ?: Arguments.LogLevel.WARN
    initLogs(logLevel)

    logi("BoD ws-to-minitel v1.0.0")

    minitel.connect {
      disableAcknowledgement()
      disableEnableLocalEcho(arguments.localEcho)
      var webSocketJob = coroutineScope.launch { keepWebSocketClientConnected(this@connect) }

      system.collect { systemEvent ->
        if (systemEvent is Minitel.SystemEvent.TurnedOnEvent) {
          logd("Turned on: restarting WebSocket client")
          webSocketJob.cancel()
          disableAcknowledgement()
          disableEnableLocalEcho(arguments.localEcho)
          webSocketJob = coroutineScope.launch { keepWebSocketClientConnected(this@connect) }
        }
      }
    }
  }

  private fun initLogs(logLevel: Arguments.LogLevel) {
    System.setProperty(
      SimpleLogger.DEFAULT_LOG_LEVEL_KEY,
      when (logLevel) {
        Arguments.LogLevel.DEBUG -> "trace"
        Arguments.LogLevel.WARN -> "info"
        Arguments.LogLevel.NONE -> "off"
      },
    )
    System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true")
    System.setProperty(SimpleLogger.DATE_TIME_FORMAT_KEY, "yyyy-MM-dd HH:mm:ss")
  }

  private suspend fun Minitel.Connection.disableAcknowledgement() {
    logd("Sending disable acknowledgement command")
    screen.disableAcknowledgement()
  }

  private suspend fun Minitel.Connection.disableEnableLocalEcho(echo: Arguments.LocalEcho?) {
    when (echo) {
      Arguments.LocalEcho.ON -> {
        logd("Sending local echo ON command")
        screen.localEcho(true)
      }

      Arguments.LocalEcho.OFF -> {
        logd("Sending local echo OFF command")
        screen.localEcho(false)
      }

      null -> {}
    }
  }

  private fun createHttpClient() = HttpClient(OkHttp) {
    engine {
      config {
        sslSocketFactory(insecureSocketFactory, naiveTrustManager)
      }
    }

    install(WebSockets) {
      pingInterval = 30.seconds
    }
  }

  private suspend fun keepWebSocketClientConnected(connection: Minitel.Connection, times: Int = 10) {
    repeat(times) {
      try {
        startWebSocketClient(connection)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        logw(e, "WebSocket client failed, retrying in 10 seconds")
        delay(10.seconds)
      }
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
    if (saveFramesToFiles != null) {
      val frameNumberPadded = frameNumber.toString().padStart(3, '0')
      val dir = File(saveFramesToFiles)
      dir.mkdirs()
      val file = dir.resolve("frame-$frameNumberPadded.vdt")
      logd("Saving frame to $file")
      BufferedOutputStream(FileOutputStream(file)).use {
        it.write(readBytes)
        it.flush()
      }
      frameNumber++
    }
  }
}

suspend fun main(av: Array<String>) {
  val arguments = Arguments(av)
  WsToMinitel(arguments).run()
}
