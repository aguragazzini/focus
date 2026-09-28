package com.foco.launcher.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Environment
import android.os.StatFs

/** Local phone glances. Missing data becomes a short unavailable line. No toggles that need system rights. */
object DeviceSignals {
    fun wifiLabel(context: Context): String {
        val manager = context.applicationContext.getSystemService(WifiManager::class.java)
            ?: return "No se puede leer"
        if (!manager.isWifiEnabled) return "Apagado"
        val raw = runCatching { manager.connectionInfo?.ssid }.getOrNull()?.trim('"')
        val name = raw?.takeIf { it.isNotBlank() && !it.equals("<unknown ssid>", ignoreCase = true) && it != "0x" }
        return name ?: "Conectado"
    }

    fun dataLabel(context: Context): String {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return "No se puede leer"
        val network = manager.activeNetwork ?: return "Sin datos"
        val caps = manager.getNetworkCapabilities(network) ?: return "Sin datos"
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "Sin datos móviles"
        val rx = TrafficStats.getMobileRxBytes()
        if (rx < 0L) return "Datos móviles"
        val mb = rx / (1024L * 1024L)
        return "Datos móviles · $mb MB"
    }

    fun freeBytes(): Long {
        return runCatching {
            StatFs(Environment.getDataDirectory().path).availableBytes
        }.getOrDefault(-1L)
    }
}
