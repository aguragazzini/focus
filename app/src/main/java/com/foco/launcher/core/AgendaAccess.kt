package com.foco.launcher.core

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Runtime calendar read. The first ask uses the system dialog.
 * After a permanent denial, the next ask opens Foco's app info.
 */
object AgendaAccess {
    private const val PREFS = "foco_agenda"
    private const val ASKED = "asked"

    fun granted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun request(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (granted(activity)) return
        val permission = Manifest.permission.READ_CALENDAR
        if (wasAsked(activity) && !activity.shouldShowRequestPermissionRationale(permission)) {
            LaunchController.openAppDetails(activity)
            return
        }
        markAsked(activity)
        launcher.launch(permission)
    }

    private fun wasAsked(context: Context): Boolean {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ASKED, false)
    }

    private fun markAsked(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ASKED, true).apply()
    }
}
