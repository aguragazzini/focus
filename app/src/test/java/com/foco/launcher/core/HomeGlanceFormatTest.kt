package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeGlanceFormatTest {
    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    @Test
    fun joinsBatteryAndAShortAlarm() {
        val alarm = at(2026, 9, 23, 6, 30)
        val label = HomeGlanceFormat.formatAlarm(alarm, zone)
        assertEquals("alarma 6:30", label)
        assertEquals("78% · alarma 6:30", HomeGlanceFormat.line(78, label))
    }

    @Test
    fun eveningAlarmKeepsTwentyFourHours() {
        val alarm = at(2026, 9, 22, 21, 47)
        assertEquals("alarma 21:47", HomeGlanceFormat.formatAlarm(alarm, zone))
    }

    @Test
    fun omitsAMissingHalfAndRejectsABadPercent() {
        assertEquals("78%", HomeGlanceFormat.line(78, null))
        assertEquals("alarma 6:30", HomeGlanceFormat.line(null, "alarma 6:30"))
        assertNull(HomeGlanceFormat.line(null, null))
        assertNull(HomeGlanceFormat.line(-1, null))
        assertNull(HomeGlanceFormat.line(101, " "))
        assertTrue(HomeGlanceFormat.GLANCE_SP in 12..13)
    }

    @Test
    fun glanceSourcesStayLocal() {
        val files = listOf(
            "src/main/java/com/foco/launcher/core/HomeGlance.kt",
            "src/main/java/com/foco/launcher/core/HomeGlanceFormat.kt",
            "src/main/java/com/foco/launcher/core/HomeClock.kt",
        )
        val blob = files.joinToString("\n") { File(it).readText() }
        assertFalse(blob.contains("READ_CALENDAR"))
        assertFalse(blob.contains("AppWidgetHost"))
        assertFalse(blob.contains("http"))
        assertFalse(blob.contains("weather"))
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("READ_CALENDAR"))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()
    }
}
