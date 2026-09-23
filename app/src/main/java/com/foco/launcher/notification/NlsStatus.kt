package com.foco.launcher.notification

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.foco.launcher.core.ResolvedStart
import java.util.concurrent.atomic.AtomicLong

object NlsStatus {
    fun component(context: Context): ComponentName {
        return ComponentName(context.applicationContext, FocoNotificationListener::class.java)
    }

    fun isGranted(context: Context): Boolean {
        return runCatching { readGranted(context.applicationContext) }.getOrDefault(false)
    }

    /**
     * Package replace often leaves the grant in place and the service unbound.
     * requestRebind is a no-op while the listener is already connected.
     */
    fun requestRebind(context: Context) {
        if (Build.VERSION.SDK_INT < 24) return
        val app = context.applicationContext
        if (!isGranted(app)) return
        val now = SystemClock.elapsedRealtime()
        val last = lastRebindElapsed.get()
        if (!NlsRebindGate.allow(now, last)) return
        if (!lastRebindElapsed.compareAndSet(last, now)) return
        runCatching { NotificationListenerService.requestRebind(component(app)) }
    }

    /**
     * Opens listener access. Detail (API 30+) then the list, then generic settings.
     * Returns false only when nothing started.
     */
    fun openListenerSettings(context: Context): Boolean {
        val flat = component(context).flattenToString()
        val candidates = NlsSettingsPlan.candidates(Build.VERSION.SDK_INT, flat)
        for (candidate in candidates) {
            if (ResolvedStart.start(context, candidate.toIntent())) return true
        }
        return false
    }

    /** "Activo" only when the toggle is on, the grant is real, and the service is bound. */
    fun isFilterActive(context: Context, nlsFilterEnabled: Boolean, connected: Boolean): Boolean {
        return NlsRecovery.filterIsActive(
            granted = isGranted(context),
            filterEnabled = nlsFilterEnabled,
            connected = connected,
        )
    }

    private fun readGranted(app: Context): Boolean {
        val cn = component(app)
        if (Build.VERSION.SDK_INT >= 27) {
            val nm = app.getSystemService(NotificationManager::class.java)
            if (nm != null && nm.isNotificationListenerAccessGranted(cn)) return true
        }
        // API 27+ can lag after a sideload. The secure setting is the other signal.
        val enabled = Settings.Secure.getString(app.contentResolver, "enabled_notification_listeners")
        return NlsGrantMatch.listed(enabled, cn.packageName, cn.className)
    }

    private val lastRebindElapsed = AtomicLong(0)
}
