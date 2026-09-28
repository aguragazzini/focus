package com.foco.launcher.core

import java.net.HttpURLConnection
import java.net.URL

/**
 * Current temperature for Córdoba from Open-Meteo. No API key.
 * A failure, timeout, or offline phone returns null.
 */
object CordobaWeather {
    const val ENDPOINT =
        "https://api.open-meteo.com/v1/forecast?latitude=-31.42&longitude=-64.19" +
            "&current=temperature_2m&timezone=America%2FArgentina%2FCordoba"

    fun fetchCelsius(endpoint: String = ENDPOINT): Int? {
        return runCatching {
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode !in 200..299) return null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                PageBlockLogic.parseOpenMeteoCelsius(body)
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
