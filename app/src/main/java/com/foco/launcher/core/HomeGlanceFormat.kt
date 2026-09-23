package com.foco.launcher.core

import java.time.Instant
import java.time.ZoneId

/**
 * One muted line under the date. Battery and the next alarm, local only.
 * Calendar stays out of this build so day-1 does not ask for a calendar permission.
 */
object HomeGlanceFormat {
    const val GLANCE_SP = 13

    fun line(batteryPercent: Int?, alarmLabel: String?): String? {
        val battery = formatBattery(batteryPercent)
        val alarm = alarmLabel?.takeIf { it.isNotBlank() }
        return when {
            battery != null && alarm != null -> "$battery · $alarm"
            battery != null -> battery
            else -> alarm
        }
    }

    fun formatBattery(percent: Int?): String? {
        if (percent == null || percent !in 0..100) return null
        return "$percent%"
    }

    /** Hour is not zero-padded: 6:30, 21:47. */
    fun formatAlarm(epochMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return "alarma ${local.hour}:%02d".format(local.minute)
    }
}
