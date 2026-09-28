package com.foco.launcher.usage

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One bucket of foreground time from UsageStats. No Android types. */
data class UsageSample(
    val packageName: String,
    val foregroundMs: Long,
    val lastUsedMillis: Long = 0L,
)

data class UsageApp(
    val packageName: String,
    val label: String,
    val todayMs: Long,
    val weekMs: Long,
    val launchable: Boolean,
    val lastUsedMillis: Long = 0L,
)

/** One local day in the seven-day bar. */
data class UsageDay(
    val startMillis: Long,
    val foregroundMs: Long,
)

/** Foreground time on apps that are not in the Personal whitelist. */
data class UsageLeak(
    val count: Int,
    val ms: Long,
)

/** One daily bucket. [beginMillis] is the bucket start, not a guessed session. */
data class UsageSpan(
    val beginMillis: Long,
    val foregroundMs: Long,
)

/**
 * Today plus the six previous local midnights. Week totals include today.
 * Packages without a real label are dropped. Numbers are never invented.
 */
object UsageReport {
    const val LIMIT = 12

    /** Aggregated query from API 28. Older releases fall back to daily buckets. */
    fun aggregate(sdkInt: Int): Boolean = sdkInt >= 28

    fun bounds(nowMillis: Long, zone: ZoneId): Pair<Long, Long> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val todayStart = startOf(today, zone)
        val weekStart = startOf(today.minusDays(6), zone)
        return todayStart to weekStart
    }

    fun combine(
        today: List<UsageSample>,
        week: List<UsageSample>,
        labelOf: (String) -> String?,
        launchable: (String) -> Boolean,
    ): List<UsageApp> {
        val todayMs = sum(today)
        val weekMs = sum(week)
        val todayLast = latest(today)
        val weekLast = latest(week)
        val packages = todayMs.keys + weekMs.keys
        return packages.mapNotNull { pkg ->
            val label = labelOf(pkg)?.trim().orEmpty()
            if (label.isEmpty()) return@mapNotNull null
            val day = todayMs[pkg] ?: 0L
            val span = (weekMs[pkg] ?: 0L).coerceAtLeast(day)
            if (day <= 0L && span <= 0L) return@mapNotNull null
            UsageApp(
                packageName = pkg,
                label = label,
                todayMs = day,
                weekMs = span,
                launchable = launchable(pkg),
                lastUsedMillis = maxOf(todayLast[pkg] ?: 0L, weekLast[pkg] ?: 0L),
            )
        }.sortedWith(compareByDescending<UsageApp> { it.weekMs }.thenBy { it.label })
    }

    /**
     * Apps with time in the selected span that are absent from [personal].
     * An empty whitelist still counts: those apps are outside Personal.
     */
    fun leak(apps: List<UsageApp>, byToday: Boolean, personal: Set<String>): UsageLeak? {
        val outside = apps.filter { ms(it, byToday) > 0L && it.packageName !in personal }
        if (outside.isEmpty()) return null
        return UsageLeak(
            count = outside.size,
            ms = outside.sumOf { ms(it, byToday) },
        )
    }

    /** Sum of SCREEN_INTERACTIVE totals. No matching time means there is nothing to show. */
    fun screenInteractiveMs(events: List<Pair<Int, Long>>, interactiveType: Int): Long? {
        var sum = 0L
        var seen = false
        for ((type, ms) in events) {
            if (type != interactiveType) continue
            seen = true
            if (ms > 0L) sum += ms
        }
        return if (!seen || sum <= 0L) null else sum
    }

    /** Top apps for the selected span. Zero in that span is omitted. */
    fun rank(apps: List<UsageApp>, byToday: Boolean): List<UsageApp> {
        return apps
            .filter { ms(it, byToday) > 0L }
            .sortedWith(compareByDescending<UsageApp> { ms(it, byToday) }.thenBy { it.label })
            .take(LIMIT)
    }

    /** Sum of every named app in the span, not only the visible rows. */
    fun totalMs(apps: List<UsageApp>, byToday: Boolean): Long {
        return apps.sumOf { ms(it, byToday).coerceAtLeast(0L) }
    }

    private fun ms(app: UsageApp, byToday: Boolean): Long {
        return if (byToday) app.todayMs else app.weekMs
    }

    /**
     * Seven local days, oldest first, ending today. A day with no bucket is 0.
     * Days outside that window are dropped, not moved.
     */
    fun dayTotals(spans: List<UsageSpan>, nowMillis: Long, zone: ZoneId): List<UsageDay> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
        val sums = HashMap<LocalDate, Long>()
        for (span in spans) {
            if (span.foregroundMs <= 0L || span.beginMillis <= 0L) continue
            val day = Instant.ofEpochMilli(span.beginMillis).atZone(zone).toLocalDate()
            if (day !in days) continue
            sums[day] = (sums[day] ?: 0L) + span.foregroundMs
        }
        return days.map { day ->
            UsageDay(startMillis = startOf(day, zone), foregroundMs = sums[day] ?: 0L)
        }
    }

    private fun sum(samples: List<UsageSample>): Map<String, Long> {
        val out = HashMap<String, Long>()
        for (sample in samples) {
            val pkg = sample.packageName.trim()
            if (pkg.isEmpty() || sample.foregroundMs <= 0L) continue
            out[pkg] = (out[pkg] ?: 0L) + sample.foregroundMs
        }
        return out
    }

    private fun latest(samples: List<UsageSample>): Map<String, Long> {
        val out = HashMap<String, Long>()
        for (sample in samples) {
            val pkg = sample.packageName.trim()
            if (pkg.isEmpty() || sample.lastUsedMillis <= 0L) continue
            out[pkg] = maxOf(out[pkg] ?: 0L, sample.lastUsedMillis)
        }
        return out
    }

    private fun startOf(day: LocalDate, zone: ZoneId): Long {
        return day.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

/** Floor to minutes. A non-zero remainder under a minute stays visible. */
object UsageFormat {
    fun duration(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        if (safe in 1L until 60_000L) return "< 1 min"
        val totalMin = safe / 60_000L
        val hours = totalMin / 60L
        val minutes = totalMin % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours h $minutes min"
            hours > 0L -> "$hours h"
            else -> "$minutes min"
        }
    }

    /** Relative local time. A missing or future stamp is omitted. */
    fun lastUsed(lastMillis: Long, nowMillis: Long, zone: ZoneId): String? {
        if (lastMillis <= 0L || nowMillis < lastMillis) return null
        val lastDate = Instant.ofEpochMilli(lastMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val delta = nowMillis - lastMillis
        if (lastDate == today) {
            if (delta < 60_000L) return "hace < 1 min"
            val minutes = delta / 60_000L
            if (minutes < 60L) return "hace $minutes min"
            val hours = minutes / 60L
            val rest = minutes % 60L
            return if (rest == 0L) "hace $hours h" else "hace $hours h $rest min"
        }
        val clock = clock(lastMillis, zone)
        if (lastDate == today.minusDays(1)) return "ayer $clock"
        return "${weekday(lastMillis, zone)} $clock"
    }

    fun weekday(startMillis: Long, zone: ZoneId): String {
        val day = Instant.ofEpochMilli(startMillis).atZone(zone).dayOfWeek
        return when (day) {
            DayOfWeek.MONDAY -> "lun"
            DayOfWeek.TUESDAY -> "mar"
            DayOfWeek.WEDNESDAY -> "mié"
            DayOfWeek.THURSDAY -> "jue"
            DayOfWeek.FRIDAY -> "vie"
            DayOfWeek.SATURDAY -> "sáb"
            DayOfWeek.SUNDAY -> "dom"
        }
    }

    private fun clock(epochMillis: Long, zone: ZoneId): String {
        val local = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()
        return "%02d:%02d".format(local.hour, local.minute)
    }
}
