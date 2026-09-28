package com.foco.launcher.core

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Local block helpers. No network and no Android framework. */
object PageBlockLogic {
    private val esAr = Locale("es", "AR")

    /** New moon at 2000-01-06 18:14 UTC. Synodic month in days. */
    private const val NEW_MOON_EPOCH = 947182440000L
    private const val SYNODIC_MS = 29.530588853 * 24.0 * 60.0 * 60.0 * 1000.0

    const val MOON_NEW = 0
    const val MOON_WAXING = 1
    const val MOON_FULL = 2
    const val MOON_WANING = 3

    fun moonPhase(epochMillis: Long): Int {
        val age = ((epochMillis - NEW_MOON_EPOCH) % SYNODIC_MS + SYNODIC_MS) % SYNODIC_MS
        val day = age / (24.0 * 60.0 * 60.0 * 1000.0)
        return when {
            day < 1.85 || day >= 27.68 -> MOON_NEW
            day < 14.76 -> MOON_WAXING
            day < 16.61 -> MOON_FULL
            else -> MOON_WANING
        }
    }

    fun moonLabel(phase: Int): String {
        return when (phase) {
            MOON_NEW -> "Luna nueva"
            MOON_WAXING -> "Creciente"
            MOON_FULL -> "Luna llena"
            else -> "Menguante"
        }
    }

    fun weekNumber(date: LocalDate): Int {
        val week = java.time.temporal.WeekFields.of(esAr).weekOfWeekBasedYear()
        return week.getFrom(date).toInt()
    }

    fun weekdayShort(date: LocalDate): String {
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "lun"
            java.time.DayOfWeek.TUESDAY -> "mar"
            java.time.DayOfWeek.WEDNESDAY -> "mié"
            java.time.DayOfWeek.THURSDAY -> "jue"
            java.time.DayOfWeek.FRIDAY -> "vie"
            java.time.DayOfWeek.SATURDAY -> "sáb"
            java.time.DayOfWeek.SUNDAY -> "dom"
        }
    }

    /** Breakfast before 11, lunch before 16, dinner after. Weekend has no slot. */
    fun mealSlot(hour: Int, weekend: Boolean): String {
        if (weekend) return "finde"
        return when {
            hour < 11 -> "desayuno"
            hour < 16 -> "almuerzo"
            else -> "cena"
        }
    }

    fun pickPhrase(lines: List<String>, date: LocalDate): String? {
        val clean = lines.map { it.trim() }.filter { it.isNotEmpty() }
        if (clean.isEmpty()) return null
        return clean[Math.floorMod(date.dayOfYear, clean.size)]
    }

    fun parseOpenMeteoCelsius(body: String): Int? {
        val marker = "\"temperature_2m\":"
        val at = body.indexOf(marker)
        if (at < 0) return null
        val raw = body.substring(at + marker.length).trimStart().takeWhile { it.isDigit() || it == '-' || it == '.' }
        val value = raw.toDoubleOrNull() ?: return null
        if (value < -80.0 || value > 60.0) return null
        return value.roundToInt()
    }

    fun nextUpcoming(events: List<AgendaEvent>, nowMillis: Long): AgendaEvent? {
        if (events.isEmpty()) return null
        return events.filter { it.endMillis > nowMillis }.minByOrNull { it.beginMillis }
            ?: events.minByOrNull { it.beginMillis }
    }

    /** A reminder-like row, or the first all-day event. Never invented. */
    fun reminder(events: List<AgendaEvent>): AgendaEvent? {
        val named = events.firstOrNull { event ->
            val title = event.title.lowercase(esAr)
            title.contains("record") || title.contains("cumple") || title.contains("aviso")
        }
        return named ?: events.firstOrNull { it.allDay }
    }

    fun storageLabel(freeBytes: Long): String? {
        if (freeBytes < 0L) return null
        val gb = freeBytes / (1024.0 * 1024.0 * 1024.0)
        return String.format(esAr, "%.1f GB libres", gb)
    }
}
