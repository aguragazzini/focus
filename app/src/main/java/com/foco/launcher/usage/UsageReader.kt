package com.foco.launcher.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZoneId

data class UsageSnapshot(
    val granted: Boolean,
    val apps: List<UsageApp>,
)

/**
 * Foreground time for this process's user. The app's own [UsageStatsManager]
 * is that user. Work-profile stats are a different user and are not queried.
 */
object UsageReader {
    fun load(context: Context, nowMillis: Long, zone: ZoneId): UsageSnapshot {
        val app = context.applicationContext
        if (!UsageAccess.isGranted(app)) return UsageSnapshot(granted = false, apps = emptyList())
        val manager = app.getSystemService(UsageStatsManager::class.java)
            ?: return UsageSnapshot(granted = true, apps = emptyList())
        val (todayStart, weekStart) = UsageReport.bounds(nowMillis, zone)
        val today = read(manager, todayStart, nowMillis) ?: return UsageSnapshot(granted = false, apps = emptyList())
        val week = read(manager, weekStart, nowMillis) ?: return UsageSnapshot(granted = false, apps = emptyList())
        val pm = app.packageManager
        return UsageSnapshot(
            granted = true,
            apps = UsageReport.combine(
                today = today,
                week = week,
                labelOf = { pkg -> label(pm, pkg) },
                launchable = { pkg -> pm.getLaunchIntentForPackage(pkg) != null },
            ),
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
                    sample(stat.packageName, stat.totalTimeInForeground)
                }
            } else {
                manager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty().map { stat ->
                    sample(stat.packageName, stat.totalTimeInForeground)
                }
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun sample(packageName: String?, foregroundMs: Long): UsageSample {
        return UsageSample(
            packageName = packageName.orEmpty(),
            foregroundMs = foregroundMs.coerceAtLeast(0L),
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
