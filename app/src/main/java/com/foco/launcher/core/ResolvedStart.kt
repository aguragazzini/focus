package com.foco.launcher.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Starts an implicit system intent, preferring the resolved component.
 * On API 30+ an implicit settings intent from a third-party HOME can no-op
 * even when the activity exists. An explicit component is the same target.
 */
object ResolvedStart {
    fun start(context: Context, intent: Intent): Boolean {
        val base = Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val explicit = explicitOrNull(context, base)
        if (explicit != null && startOnce(context, explicit)) return true
        return startOnce(context, base)
    }

    private fun explicitOrNull(context: Context, intent: Intent): Intent? {
        val info = runCatching {
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
        }.getOrNull() ?: return null
        val pkg = info.packageName ?: return null
        val cls = info.name ?: return null
        if (pkg == "android") return null
        return Intent(intent).setComponent(ComponentName(pkg, cls))
    }

    private fun startOnce(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
