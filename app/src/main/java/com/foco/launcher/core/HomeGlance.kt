package com.foco.launcher.core

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.time.ZoneId

/** Reads battery and the next alarm. No calendar permission and no network. */
object HomeGlance {
    fun line(context: Context, zone: ZoneId = ZoneId.systemDefault()): String? {
        return runCatching {
            HomeGlanceFormat.line(batteryPercent(context), alarmLabel(context, zone))
        }.getOrNull()
    }

    private fun batteryPercent(context: Context): Int? {
        val manager = context.getSystemService(BatteryManager::class.java)
        val direct = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (direct != null && direct in 0..100) return direct
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return null
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return ((level * 100) / scale).takeIf { it in 0..100 }
    }

    private fun alarmLabel(context: Context, zone: ZoneId): String? {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return null
        val trigger = manager.nextAlarmClock?.triggerTime ?: return null
        if (trigger <= 0L) return null
        return HomeGlanceFormat.formatAlarm(trigger, zone)
    }
}
