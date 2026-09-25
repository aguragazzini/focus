package com.foco.launcher.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.util.Locale

/**
 * One temperature figure for the home glance. No location permission:
 * the zone id picks a coarse point, and a failure leaves the line out.
 */
object LocalTemperature {
    private val points = mapOf(
        "America/Argentina/Buenos_Aires" to (-34.61 to -58.38),
        "America/Argentina/Cordoba" to (-31.42 to -64.18),
        "America/Argentina/Mendoza" to (-32.89 to -68.85),
        "America/Argentina/Salta" to (-24.79 to -65.41),
        "America/Argentina/Tucuman" to (-26.81 to -65.22),
        "America/Argentina/San_Juan" to (-31.54 to -68.54),
        "America/Argentina/Jujuy" to (-24.19 to -65.30),
        "America/Argentina/La_Rioja" to (-29.41 to -66.86),
        "America/Argentina/Catamarca" to (-28.47 to -65.78),
        "America/Argentina/San_Luis" to (-33.30 to -66.34),
        "America/Argentina/Rio_Gallegos" to (-51.62 to -69.22),
        "America/Argentina/Ushuaia" to (-54.81 to -68.32),
        "America/Montevideo" to (-34.90 to -56.16),
        "America/Santiago" to (-33.45 to -70.67),
        "America/Sao_Paulo" to (-23.55 to -46.63),
        "America/Mexico_City" to (19.43 to -99.13),
        "America/Bogota" to (4.71 to -74.07),
        "America/Lima" to (-12.05 to -77.04),
        "America/New_York" to (40.71 to -74.01),
        "America/Chicago" to (41.88 to -87.63),
        "America/Denver" to (39.74 to -104.99),
        "America/Los_Angeles" to (34.05 to -118.24),
        "Europe/Madrid" to (40.42 to -3.70),
        "Europe/Rome" to (41.90 to 12.50),
        "Europe/Paris" to (48.86 to 2.35),
        "Europe/London" to (51.51 to -0.13),
    )

    @Volatile
    private var cachedZone: String? = null

    @Volatile
    private var cachedAt: Long = 0L

    @Volatile
    private var cachedLabel: String? = null

    fun label(zone: ZoneId, nowMillis: Long = System.currentTimeMillis()): String? {
        val point = pointFor(zone) ?: return null
        val fresh = cachedZone == zone.id && nowMillis - cachedAt < TTL_MS && cachedLabel != null
        if (fresh) return cachedLabel
        val next = fetch(point.first, point.second) ?: return cachedLabel
        cachedZone = zone.id
        cachedAt = nowMillis
        cachedLabel = next
        return next
    }

    private fun pointFor(zone: ZoneId): Pair<Double, Double>? {
        points[zone.id]?.let { return it }
        if (zone.id.startsWith("America/Argentina")) return points["America/Argentina/Buenos_Aires"]
        return null
    }

    private fun fetch(latitude: Double, longitude: Double): String? {
        val lat = String.format(Locale.US, "%.2f", latitude)
        val lon = String.format(Locale.US, "%.2f", longitude)
        val address = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m"
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_000
            readTimeout = 4_000
            requestMethod = "GET"
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val match = TEMP.find(body) ?: return null
            val rounded = match.groupValues[1].toDouble().let { kotlin.math.round(it).toInt() }
            "$rounded°"
        } finally {
            connection.disconnect()
        }
    }

    private val TEMP = Regex(""""temperature_2m"\s*:\s*(-?\d+(?:\.\d+)?)""")
    private const val TTL_MS = 3 * 60 * 60 * 1000L
}

@Composable
fun rememberLocalTemperature(zone: ZoneId = ZoneId.systemDefault()): String? {
    var label by remember(zone.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(zone.id) {
        label = withContext(Dispatchers.IO) {
            runCatching { LocalTemperature.label(zone) }.getOrNull()
        }
    }
    return label
}
