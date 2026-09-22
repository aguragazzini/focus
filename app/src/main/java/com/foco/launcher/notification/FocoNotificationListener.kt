package com.foco.launcher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.foco.launcher.FocoApp

/**
 * Único lugar que cancela notificaciones.
 * Work profile: never cancel. No inbox. No Device Admin.
 */
class FocoNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        scrubActive()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (shouldSuppress(sbn)) {
            cancelNotification(sbn.key)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No inbox.
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
    }

    private fun scrubActive() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        for (sbn in active) {
            if (shouldSuppress(sbn)) {
                cancelNotification(sbn.key)
            }
        }
    }

    private fun shouldSuppress(sbn: StatusBarNotification): Boolean {
        val app = applicationContext as? FocoApp ?: return false
        // Fail open until migration finishes. Work profiles stay unfiltered either way.
        if (!app.startupReady.value) return false
        return app.notificationPolicy.shouldSuppress(sbn)
    }
}
