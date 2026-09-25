package com.foco.launcher.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.foco.launcher.FocoApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Only place that cancels notifications.
 * Work profile passes unless a Foco pause flag says otherwise.
 * No inbox. No device admin. No system interruption filter.
 */
class FocoNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        active.set(this)
        connectedState.value = true
        scrubActive()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!decide(sbn)) return
        runCatching { cancelNotification(sbn.key) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No inbox.
    }

    override fun onListenerDisconnected() {
        if (active.compareAndSet(this, null)) {
            connectedState.value = active.get() != null
        }
        super.onListenerDisconnected()
        // Sideload / OEM unbind: the grant can still be on while posted() never runs.
        NlsStatus.requestRebind(this)
    }

    private fun scrubActive() {
        val posted = runCatching { activeNotifications }.getOrNull() ?: return
        for (sbn in posted) {
            if (!decide(sbn)) continue
            runCatching { cancelNotification(sbn.key) }
        }
    }

    private fun decide(sbn: StatusBarNotification): Boolean {
        return runCatching { shouldSuppress(sbn) }.getOrDefault(false)
    }

    private fun shouldSuppress(sbn: StatusBarNotification): Boolean {
        val app = applicationContext as? FocoApp ?: return false
        // Fail open until migration finishes. Work profiles stay unfiltered either way.
        if (!app.startupReady.value) return false
        return app.notificationPolicy.shouldSuppress(sbn)
    }

    companion object {
        private val active = AtomicReference<FocoNotificationListener?>(null)
        private val connectedState = MutableStateFlow(false)

        /** True only while the system has this service bound. */
        val connected: StateFlow<Boolean> = connectedState.asStateFlow()

        fun isConnected(): Boolean = active.get() != null

        fun scrubIfConnected() {
            active.get()?.scrubActive()
        }
    }
}
