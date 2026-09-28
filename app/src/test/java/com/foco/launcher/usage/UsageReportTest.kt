package com.foco.launcher.usage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId

class UsageReportTest {
    private val zone = ZoneId.of("America/Argentina/Cordoba")

    @Test
    fun boundsCoverTodayAndTheSixDaysBefore() {
        val now = java.time.ZonedDateTime.of(2026, 9, 28, 15, 0, 0, 0, zone).toInstant().toEpochMilli()
        val (todayStart, weekStart) = UsageReport.bounds(now, zone)
        assertEquals(
            java.time.LocalDate.of(2026, 9, 28).atStartOfDay(zone).toInstant().toEpochMilli(),
            todayStart,
        )
        assertEquals(
            java.time.LocalDate.of(2026, 9, 22).atStartOfDay(zone).toInstant().toEpochMilli(),
            weekStart,
        )
        assertTrue(weekStart < todayStart)
        assertTrue(todayStart <= now)
    }

    @Test
    fun combineSumsBucketsDropsBlankLabelsAndCapsTheList() {
        val today = listOf(
            UsageSample("com.chrome", 3_600_000L),
            UsageSample("com.chrome", 60_000L),
            UsageSample("com.gone", 9_000_000L),
            UsageSample("  ", 5_000L),
            UsageSample("com.zero", 0L),
        )
        val week = listOf(
            UsageSample("com.chrome", 1_000L),
            UsageSample("com.maps", 120_000L),
        )
        val rows = UsageReport.combine(
            today = today,
            week = week,
            labelOf = { pkg -> if (pkg == "com.gone") "  " else pkg.substringAfter('.') },
            launchable = { it == "com.chrome" },
        )
        assertEquals(listOf("com.chrome", "com.maps"), rows.map { it.packageName })
        assertEquals(3_660_000L, rows[0].todayMs)
        assertEquals(3_660_000L, rows[0].weekMs)
        assertTrue(rows[0].launchable)
        assertEquals(0L, rows[1].todayMs)
        assertEquals(120_000L, rows[1].weekMs)
        assertEquals("maps", rows[1].label)
    }

    @Test
    fun rankFollowsTheSelectedSpanAndCapsAtTwelve() {
        val apps = (1..20).map { index ->
            UsageApp(
                packageName = "p$index",
                label = "App $index",
                todayMs = index * 1_000L,
                weekMs = (21 - index) * 1_000L,
                launchable = true,
            )
        }
        val today = UsageReport.rank(apps, byToday = true)
        val week = UsageReport.rank(apps, byToday = false)
        assertEquals(12, today.size)
        assertEquals("p20", today.first().packageName)
        assertEquals("p1", week.first().packageName)
        assertTrue(today.none { it.todayMs <= 0L })
        assertEquals((1L..20L).sum() * 1_000L, UsageReport.totalMs(apps, byToday = true))
        assertEquals((1L..20L).sum() * 1_000L, UsageReport.totalMs(apps, byToday = false))
    }

    @Test
    fun aggregateQueryStartsAtApi28() {
        assertFalse(UsageReport.aggregate(26))
        assertFalse(UsageReport.aggregate(27))
        assertTrue(UsageReport.aggregate(28))
        assertTrue(UsageReport.aggregate(34))
        val reader = File("src/main/java/com/foco/launcher/usage/UsageReader.kt").readText()
        assertTrue(reader.contains("queryAndAggregateUsageStats"))
        assertTrue(reader.contains("queryUsageStats"))
        assertFalse(reader.contains("queryEvents"))
        assertFalse(reader.contains("createContextAsUser"))
        val page = File("src/main/java/com/foco/launcher/core/UsagePage.kt").readText()
        assertFalse(page.contains("while (true)"))
        assertFalse(page.contains("delay(60_000"))
        assertTrue(page.contains("uso_refresh"))
        assertTrue(page.contains("uso_empty_today"))
        assertTrue(page.contains("uso_read_fail"))
        assertTrue(page.contains("36.sp"))
        assertTrue(page.contains("0.20f"))
        assertTrue(page.contains("if (!active) return@LaunchedEffect"))
        val home = File("src/main/java/com/foco/launcher/core/HomeScreen.kt").readText()
        assertTrue(home.contains("pagerState.settledPage == page"))
    }

    @Test
    fun dayTotalsKeepSevenLocalDaysAndDropTheRest() {
        val zone = ZoneId.of("America/Argentina/Cordoba")
        val now = java.time.ZonedDateTime.of(2026, 9, 28, 15, 0, 0, 0, zone).toInstant().toEpochMilli()
        val older = java.time.LocalDate.of(2026, 9, 21).atStartOfDay(zone).toInstant().toEpochMilli()
        val first = java.time.LocalDate.of(2026, 9, 22).atStartOfDay(zone).toInstant().toEpochMilli()
        val last = java.time.LocalDate.of(2026, 9, 28).atStartOfDay(zone).toInstant().toEpochMilli()
        val days = UsageReport.dayTotals(
            spans = listOf(
                UsageSpan(older, 9_000L),
                UsageSpan(first, 60_000L),
                UsageSpan(first + 1_000L, 30_000L),
                UsageSpan(last, 120_000L),
                UsageSpan(0L, 50_000L),
            ),
            nowMillis = now,
            zone = zone,
        )
        assertEquals(7, days.size)
        assertEquals(90_000L, days.first())
        assertEquals(0L, days[1])
        assertEquals(120_000L, days.last())
    }

    @Test
    fun durationFloorsToHoursAndKeepsAShortRemainder() {
        assertEquals("0 min", UsageFormat.duration(0L))
        assertEquals("< 1 min", UsageFormat.duration(30_000L))
        assertEquals("2 min", UsageFormat.duration(120_000L))
        assertEquals("1 h", UsageFormat.duration(3_600_000L))
        assertEquals("1 h 2 min", UsageFormat.duration(3_720_000L))
    }
}
