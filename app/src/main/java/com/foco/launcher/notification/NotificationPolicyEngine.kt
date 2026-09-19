package com.foco.launcher.notification

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import com.foco.launcher.registry.SuggestedApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicReference

class NotificationPolicyEngine(
    private val context: Context,
    allowlistStore: NotificationAllowlistStore,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val snapshot = AtomicReference(Snapshot())

    init {
        allowlistStore.prefs
            .onEach { prefs ->
                snapshot.set(
                    Snapshot(
                        nlsFilterEnabled = prefs.nlsFilterEnabled,
                        packages = prefs.packages,
                    ),
                )
            }
            .launchIn(scope)
    }

    fun shouldSuppress(sbn: StatusBarNotification): Boolean {
        val current = snapshot.get()
        val notification = sbn.notification
        val extras = notification?.extras
        val template = extras?.getString(Notification.EXTRA_TEMPLATE).orEmpty()
        val facts = NotificationPolicy.Facts(
            packageName = sbn.packageName.orEmpty(),
            isWorkOrOtherProfile = UserProfileHelper.isWorkOrOtherProfile(sbn),
            category = notification?.category,
            isMediaStyle = template.contains("MediaStyle"),
            hasMediaSession = extras?.containsKey(Notification.EXTRA_MEDIA_SESSION) == true,
            isDialerPackage = SuggestedApps.isPhone(appContext, sbn.packageName),
        )
        return NotificationPolicy.shouldSuppress(
            facts = facts,
            nlsFilterEnabled = current.nlsFilterEnabled,
            allowlist = current.packages,
        )
    }

    private data class Snapshot(
        val nlsFilterEnabled: Boolean = false,
        val packages: Set<String> = emptySet(),
    )
}
