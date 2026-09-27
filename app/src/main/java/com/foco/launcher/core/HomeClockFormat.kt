package com.foco.launcher.core

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * System clock only. No network and no climate provider. No seconds.
 * Time is 24h local. Date is the short Spanish form from the v0.5.1 spec (`mar 22 sep`).
 */
object HomeClockFormat {
    const val TIME_SP = 52
    const val DATE_SP = 14

    /** Civil clock for Reloj and both sanctorales. Same zone as the diet week. */
    val CIVIL_ZONE: ZoneId = ZoneId.of("America/Argentina/Cordoba")

    fun time(epochMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return "%02d:%02d".format(local.hour, local.minute)
    }

    fun date(epochMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val dow = when (local.dayOfWeek) {
            DayOfWeek.MONDAY -> "lun"
            DayOfWeek.TUESDAY -> "mar"
            DayOfWeek.WEDNESDAY -> "mié"
            DayOfWeek.THURSDAY -> "jue"
            DayOfWeek.FRIDAY -> "vie"
            DayOfWeek.SATURDAY -> "sáb"
            DayOfWeek.SUNDAY -> "dom"
        }
        val month = MONTHS[local.monthValue - 1]
        return "$dow ${local.dayOfMonth} $month"
    }

    private val MONTHS = arrayOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    )
}
