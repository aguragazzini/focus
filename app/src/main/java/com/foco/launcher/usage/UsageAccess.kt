package com.foco.launcher.usage

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import com.foco.launcher.core.LaunchController
import com.foco.launcher.core.ResolvedStart

/** Special access, same idea as the notification listener. Missing grant is not a crash. */
object UsageAccess {
    fun isGranted(context: Context): Boolean {
        val app = context.applicationContext
        val ops = app.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), app.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** This app's usage-access screen, then the list, then Foco's app info. */
    fun openSettings(context: Context): Boolean {
        val specific = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
        if (ResolvedStart.start(context, specific)) return true
        val list = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (ResolvedStart.start(context, list)) return true
        return LaunchController.openAppDetails(context)
    }
}
