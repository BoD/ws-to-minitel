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
import io.ktor.websocket.readBytes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jraf.wstominitel.arguments.Arguments
import org.jraf.wstominitel.util.insecureSocketFactory
import org.jraf.wstominitel.util.logd
import org.jraf.wstominitel.util.naiveTrustManager
import org.jraf.wstominitel.util.readWithTimeout
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class)
private class WsToMinitel(
  private val arguments: Arguments,
) {
  private val output: OutputStream = arguments.output?.let { FileOutputStream(it) } ?: System.out
  private val input: InputStream = arguments.input?.let { FileInputStream(it) } ?: System.`in`

  private val keyboardChannel = Channel<ByteArray>(0)

  private var frameNumber = 0

  suspend fun run() {
    disableAcknowledgement()
    disableEnableLocalEcho()
    startKeyboardLoopThread()
    startWebSocketClient()
  }

  private fun disableAcknowledgement() {
    logd("Sending disable acknowledgement command")
    // ESC, PRO2, NON_DIFFUSION, EMET_ECRAN
    output.write(byteArrayOf(0x1B, 0x3A, 0x64, 0x50))

    // ESC, PRO2, NON_DIFFUSION, EMET_PRISE
    output.write(byteArrayOf(0x1B, 0x3A, 0x64, 0x53))
    output.flush()
  }

  private fun disableEnableLocalEcho() {
    when (arguments.localEcho) {
      Arguments.LocalEcho.ON -> {
        logd("Sending local echo ON command")
        // ESC, PRO3, AIGUILLAGE_ON, RCPT_ECRAN, EMET_MODEM
        output.write(byteArrayOf(0x1B, 0x3B, 0x61, 0x58, 0x52))
        output.flush()
      }

      Arguments.LocalEcho.OFF -> {
        logd("Sending local echo OFF command")
        // ESC, PRO3, AIGUILLAGE_OFF, RCPT_ECRAN, EMET_MODEM
        output.write(byteArrayOf(0x1B, 0x3B, 0x60, 0x58, 0x52))
        output.flush()
      }

      null -> {}
    }
  }

  private fun startKeyboardLoopThread() {
    thread(name = "ws-to-minitel Read Keyboard Loop") {
      while (true) {
        val firstByte = input.read()
        if (firstByte == -1) {
          logd("End of keyboard input")
          break
        }
        val isPrintable = firstByte in 0x20..0x7E
        var remainingBytes = ByteArray(8)
        if (!isPrintable) {
          // We got a special key, read the next few bytes before sending them
          val len = input.readWithTimeout(remainingBytes, 500.milliseconds)
          if (len == -1) {
            logd("End of keyboard input")
            break
          }
          remainingBytes = remainingBytes.copyOf(len)
        }
        val buffer = if (!isPrintable) {
          byteArrayOf(firstByte.toByte()) + remainingBytes
        } else {
          byteArrayOf(firstByte.toByte())
        }

        @OptIn(ExperimentalStdlibApi::class)
        logd("Read from keyboard: ${buffer.size} bytes (${buffer.joinToString { it.toHexString() }})")
        runBlocking {
          keyboardChannel.send(buffer)
        }
      }
    }
  }

  private suspend fun startWebSocketClient() {
    val client = HttpClient(OkHttp) {
      engine {
        config {
          sslSocketFactory(insecureSocketFactory, naiveTrustManager)
        }
      }

      install(WebSockets) {
        pingInterval = 30.seconds
      }
    }

    client.use {
      it.webSocket(arguments.url) {
        launch {
          for (readBytes in keyboardChannel) {
            logd("Sending: ${readBytes.size} keyboard bytes")
            outgoing.send(Frame.Text(true, readBytes))
          }
        }

        while (true) {
          when (val frame = incoming.receive()) {
            is Frame.Text -> {
              val data = frame.readBytes()
              logd("Received Text frame (${data.size} bytes)")
              saveFrame(data)
              output.write(data)
              output.flush()
            }

            is Frame.Binary -> {
              val data = frame.data
              logd("Received Binary frame (${data.size} bytes)")
              saveFrame(data)
              output.write(data)
              output.flush()
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
    if (arguments.saveFramesToFiles != null) {
      val frameNumberPadded = frameNumber.toString().padStart(3, '0')
      val dir = File(arguments.saveFramesToFiles)
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
  logd("BoD ws-to-minitel v1.0.0")
  val arguments = Arguments(av)
  WsToMinitel(arguments).run()
}
