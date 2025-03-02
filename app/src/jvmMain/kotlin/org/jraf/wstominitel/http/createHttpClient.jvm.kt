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

package org.jraf.wstominitel.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.time.Duration.Companion.seconds

private val naiveTrustManager = object : X509TrustManager {
  override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
  override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
  override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
}

private val insecureSocketFactory = SSLContext.getInstance("TLS").apply {
  val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
  init(null, trustAllCerts, SecureRandom())
}.socketFactory

actual fun createHttpClient(): HttpClient {
  return HttpClient(OkHttp) {
    engine {
      config {
        sslSocketFactory(insecureSocketFactory, naiveTrustManager)
      }
    }

    install(WebSockets) {
      pingInterval = 30.seconds
    }
  }
}
