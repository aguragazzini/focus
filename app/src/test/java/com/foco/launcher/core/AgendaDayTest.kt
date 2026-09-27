package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AgendaDayTest {
    private val zone = ZoneId.of("America/Argentina/Cordoba")
    private val day = LocalDate.of(2026, 9, 26)

    @Test
    fun cordobaDayAndClockStayOnTheCivilDate() {
        val noon = day.atTime(LocalTime.NOON).atZone(zone).toInstant().toEpochMilli()
        assertEquals(day, AgendaDay.dayOf(noon, zone))
        assertEquals("12:00", AgendaDay.clock(noon, zone))
        val lateUtc = day.atTime(1, 30).atZone(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(LocalDate.of(2026, 9, 25), AgendaDay.dayOf(lateUtc, zone))
        assertEquals("22:30", AgendaDay.clock(lateUtc, zone))
    }

    @Test
    fun allDayUtcMidnightStillCoversTheCordobaDate() {
        val begin = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertTrue(AgendaDay.covers(day, begin, end, allDay = true, zone))
        assertFalse(AgendaDay.covers(day.minusDays(1), begin, end, allDay = true, zone))
        assertFalse(AgendaDay.covers(day.plusDays(1), begin, end, allDay = true, zone))
        val window = AgendaDay.queryWindow(day, zone)
        assertTrue(begin in window)
        assertTrue(end in window)
    }

    @Test
    fun timedEventsUseTheCordobaMidnightWindow() {
        val inside = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val later = day.atTime(10, 30).atZone(zone).toInstant().toEpochMilli()
        assertTrue(AgendaDay.covers(day, inside, later, allDay = false, zone))
        val previousNight = day.minusDays(1).atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        val justAfterMidnight = day.atTime(0, 30).atZone(zone).toInstant().toEpochMilli()
        assertTrue(AgendaDay.covers(day, previousNight, justAfterMidnight, allDay = false, zone))
        val endsAtMidnight = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val startedYesterday = day.minusDays(1).atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        assertFalse(AgendaDay.covers(day, startedYesterday, endsAtMidnight, allDay = false, zone))
    }

    @Test
    fun selectDropsCanceledKeepsSideAndSortsAllDayFirst() {
        val allDayBegin = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val allDayEnd = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val morning = day.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val noon = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val rows = listOf(
            AgendaRow(2, "Standup", "Trabajo", morning, noon, false, 0, null, AgendaSide.PERSONAL),
            AgendaRow(1, "Feriado", "Feriados", allDayBegin, allDayEnd, true, 0xFF336699.toInt(), null, AgendaSide.PERSONAL),
            AgendaRow(3, "Cancelada", "Personal", morning, noon, false, 0, AgendaDay.STATUS_CANCELED, AgendaSide.PERSONAL),
            AgendaRow(2, "Standup", "Trabajo", morning, noon, false, 0, null, AgendaSide.PERSONAL),
            AgendaRow(9, "Review", "", morning + 3_600_000, noon, false, 0, null, AgendaSide.WORK),
            AgendaRow(8, "Ayer", "Personal", morning - 86_400_000, morning - 80_000_000, false, 0, null, AgendaSide.PERSONAL),
        )
        val picked = AgendaDay.select(rows, day, zone)
        assertEquals(listOf(1L, 2L, 9L), picked.map { it.eventId })
        assertEquals(AgendaSide.PERSONAL, picked[1].side)
        assertEquals("Trabajo", picked[1].calendarName)
        assertEquals(AgendaSide.WORK, picked.last().side)
        assertTrue(picked.first().allDay)
        assertFalse(picked.any { it.title == "Cancelada" })
        assertFalse(picked.any { it.title == "Ayer" })
    }
}
