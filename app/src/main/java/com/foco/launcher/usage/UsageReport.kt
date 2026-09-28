package com.foco.launcher.usage

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One bucket of foreground time from UsageStats. No Android types. */
data class UsageSample(
    val packageName: String,
    val foregroundMs: Long,
)

data class UsageApp(
    val packageName: String,
    val label: String,
    val todayMs: Long,
    val weekMs: Long,
    val launchable: Boolean,
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
            )
        }.sortedWith(compareByDescending<UsageApp> { it.weekMs }.thenBy { it.label })
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

    private fun sum(samples: List<UsageSample>): Map<String, Long> {
        val out = HashMap<String, Long>()
        for (sample in samples) {
            val pkg = sample.packageName.trim()
            if (pkg.isEmpty() || sample.foregroundMs <= 0L) continue
            out[pkg] = (out[pkg] ?: 0L) + sample.foregroundMs
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
}
