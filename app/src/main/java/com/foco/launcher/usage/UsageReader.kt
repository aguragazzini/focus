package com.foco.launcher.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZoneId

data class UsageSnapshot(
    val granted: Boolean,
    val apps: List<UsageApp>,
    /** Seven local days, oldest first. Empty when the daily buckets were not read. */
    val days: List<UsageDay> = emptyList(),
    /** A read failed after the grant. This is not zero usage. */
    val failed: Boolean = false,
    /** When this snapshot was read. Relative "hace…" lines use this, not a ticker. */
    val readAtMillis: Long = 0L,
    /** Screen interactive time for local today. Null when the OEM returned nothing. */
    val screenOnTodayMs: Long? = null,
)

/**
 * Foreground time for this process's user. The app's own [UsageStatsManager]
 * is that user. Work-profile stats are a different user and are not queried.
 */
object UsageReader {
    fun load(context: Context, nowMillis: Long, zone: ZoneId): UsageSnapshot {
        return try {
            loadGranted(context, nowMillis, zone)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: SecurityException) {
            UsageSnapshot(granted = false, apps = emptyList())
        } catch (_: Exception) {
            UsageSnapshot(granted = true, failed = true, apps = emptyList())
        }
    }

    private fun loadGranted(context: Context, nowMillis: Long, zone: ZoneId): UsageSnapshot {
        val app = context.applicationContext
        if (!UsageAccess.isGranted(app)) return UsageSnapshot(granted = false, apps = emptyList())
        val manager = app.getSystemService(UsageStatsManager::class.java)
            ?: return UsageSnapshot(granted = true, apps = emptyList())
        val (todayStart, weekStart) = UsageReport.bounds(nowMillis, zone)
        val today = read(manager, todayStart, nowMillis) ?: return UsageSnapshot(granted = false, apps = emptyList())
        val week = read(manager, weekStart, nowMillis) ?: return UsageSnapshot(granted = false, apps = emptyList())
        val spans = readDaily(manager, weekStart, nowMillis) ?: return UsageSnapshot(granted = false, apps = emptyList())
        val pm = app.packageManager
        return UsageSnapshot(
            granted = true,
            apps = UsageReport.combine(
                today = today,
                week = week,
                labelOf = { pkg -> label(pm, pkg) },
                launchable = { pkg -> pm.getLaunchIntentForPackage(pkg) != null },
            ),
            days = UsageReport.dayTotals(spans, nowMillis, zone),
            readAtMillis = nowMillis,
            screenOnTodayMs = screenOnToday(manager, todayStart, nowMillis),
        )
    }

    /**
     * Null means the grant was refused while reading, so the page must not
     * present that as zero usage. Events are not queried.
     */
    private fun read(manager: UsageStatsManager, start: Long, end: Long): List<UsageSample>? {
        if (end <= start) return emptyList()
        return try {
            if (UsageReport.aggregate(Build.VERSION.SDK_INT)) {
                manager.queryAndAggregateUsageStats(start, end).orEmpty().map { (_, stat) ->
                    sample(stat.packageName, stat.totalTimeInForeground, stat.lastTimeUsed)
                }
            } else {
                manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty().map { stat ->
                    sample(stat.packageName, stat.totalTimeInForeground, stat.lastTimeUsed)
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }

    /** Daily buckets for the seven-day bar. Totals stay on the aggregated read. */
    private fun readDaily(manager: UsageStatsManager, start: Long, end: Long): List<UsageSpan>? {
        if (end <= start) return emptyList()
        return try {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty().map { stat ->
                UsageSpan(
                    beginMillis = stat.firstTimeStamp,
                    foregroundMs = stat.totalTimeInForeground.coerceAtLeast(0L),
                )
            }
        } catch (_: SecurityException) {
            null
        }
    }

    /**
     * Screen-on for local today. Empty or unavailable stays null.
     * This is not a session timeline.
     */
    private fun screenOnToday(manager: UsageStatsManager, start: Long, end: Long): Long? {
        if (Build.VERSION.SDK_INT < 28 || end <= start) return null
        return try {
            val rows = manager.queryEventStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
            UsageReport.screenInteractiveMs(
                rows.map { it.eventType to it.totalTime },
                UsageEvents.Event.SCREEN_INTERACTIVE,
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
    }

    private fun sample(packageName: String?, foregroundMs: Long, lastUsedMillis: Long): UsageSample {
        return UsageSample(
            packageName = packageName.orEmpty(),
            foregroundMs = foregroundMs.coerceAtLeast(0L),
            lastUsedMillis = lastUsedMillis.coerceAtLeast(0L),
        )
    }

    private fun label(pm: PackageManager, packageName: String): String? {
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}
