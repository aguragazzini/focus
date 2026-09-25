package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertNotEquals
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HomeClockFormatTest {
    private val zone = ZoneOffset.ofHours(-3)

    @Test
    fun formats24hTimeAndShortSpanishDate() {
        val tuesday = ZonedDateTime.of(2026, 9, 22, 21, 47, 30, 0, zone)
        assertEquals("21:47", HomeClockFormat.time(tuesday.toInstant().toEpochMilli(), zone))
        assertEquals("mar 22 sep", HomeClockFormat.date(tuesday.toInstant().toEpochMilli(), zone))
    }

    @Test
    fun dropsSecondsAndPadsMidnight() {
        val midnight = ZonedDateTime.of(2026, 9, 27, 0, 5, 59, 0, zone)
        assertEquals("00:05", HomeClockFormat.time(midnight.toInstant().toEpochMilli(), zone))
        assertEquals("dom 27 sep", HomeClockFormat.date(midnight.toInstant().toEpochMilli(), zone))
        assertFalse(HomeClockFormat.time(midnight.toInstant().toEpochMilli(), zone).contains("59"))
    }

    @Test
    fun cordobaMidnightChangesTheCivilDate() {
        val zone = HomeClockFormat.CIVIL_ZONE
        val before = ZonedDateTime.of(2026, 9, 25, 23, 59, 0, 0, zone).toInstant().toEpochMilli()
        val after = ZonedDateTime.of(2026, 9, 26, 0, 1, 0, 0, zone).toInstant().toEpochMilli()
        assertNotEquals(HomeClockFormat.date(before, zone), HomeClockFormat.date(after, zone))
        assertEquals("vie 25 sep", HomeClockFormat.date(before, zone))
        assertEquals("sáb 26 sep", HomeClockFormat.date(after, zone))
    }

    @Test
    fun typeSizesMatchTheHomeSpec() {
        assertTrue(HomeClockFormat.TIME_SP in 48..56)
        assertTrue(HomeClockFormat.DATE_SP in 13..14)
    }
}
