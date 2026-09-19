package com.foco.launcher.notification

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object NlsStatus {
    fun component(context: Context): ComponentName {
        return ComponentName(context, FocoNotificationListener::class.java)
    }

    fun isGranted(context: Context): Boolean {
        val app = context.applicationContext
        val cn = component(app)
        if (Build.VERSION.SDK_INT >= 27) {
            val nm = app.getSystemService(NotificationManager::class.java)
            if (nm != null) {
                return nm.isNotificationListenerAccessGranted(cn)
            }
        }
        val enabled = Settings.Secure.getString(
            app.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val flattened = cn.flattenToString()
        val shortName = "${cn.packageName}/${cn.className}"
        return enabled.split(':').any { entry ->
            entry.equals(flattened, ignoreCase = true) ||
                entry.equals(shortName, ignoreCase = true)
        }
    }

    fun openListenerSettings(context: Context): Boolean {
        val specific = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(specific)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /** Filtering is only "active" with both the flag and the system grant. */
    fun isFilterActive(context: Context, nlsFilterEnabled: Boolean): Boolean {
        return nlsFilterEnabled && isGranted(context)
    }
}
