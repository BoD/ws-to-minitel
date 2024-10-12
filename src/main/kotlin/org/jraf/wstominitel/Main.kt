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
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.time.Duration.Companion.seconds

suspend fun main(av: Array<String>) {
  logd("BoD ws-to-minitel v1.0.0")
  val arguments = Arguments(av)

  val output = BufferedOutputStream(FileOutputStream(arguments.ttyPath))
  val input = FileInputStream(File(arguments.ttyPath))

  val keyboardChannel = Channel<ByteArray>(0)

  when (arguments.localEcho) {
    Arguments.LocalEcho.ON -> {
      logd("Sending local echo ON command")
      // ESC, PRO3, AIGUILLAGE_ON, RCPT_ECRAN, EMET_MODEM
      output.write(byteArrayOf(0x1B, 0x3B, 0x61, 0x58, 0x52))
      output.flush()

      // We get 5 bytes back when we change the local echo setting, consume them
      repeat(5) { input.read() }
    }

    Arguments.LocalEcho.OFF -> {
      logd("Sending local echo OFF command")
      // ESC, PRO3, AIGUILLAGE_OFF, RCPT_ECRAN, EMET_MODEM
      output.write(byteArrayOf(0x1B, 0x3B, 0x60, 0x58, 0x52))
      output.flush()

      // We get 5 bytes back when we change the local echo setting, consume them
      repeat(5) { input.read() }
    }

    null -> {}
  }

  thread(name = "ws-to-minitel Read Keyboard Loop") {
    val buffer = ByteArray(50)
    while (true) {
      val len = input.read(buffer)
      if (len == -1) {
        logd("End of keyboard input")
        break
      }
      logd("Read from keyboard: $len bytes")
      runBlocking {
        keyboardChannel.send(buffer.copyOf(len))
      }
    }
  }

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
            logd("Received Text frame")
            val readBytes = frame.readBytes()
            if (arguments.verbose) {
              logd("Contents:\n${String(readBytes)}")
            }
            output.write(readBytes)
            output.flush()
          }

          is Frame.Binary -> {
            logd("Received Binary frame")
            val data = frame.data
            if (arguments.verbose) {
              logd("Contents:\n${data.size} bytes")
            }
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
