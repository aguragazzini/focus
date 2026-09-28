package com.foco.launcher

import android.app.Application
import com.foco.launcher.core.DietPlan
import com.foco.launcher.core.HomeGlance
import com.foco.launcher.core.Santoral1962
import com.foco.launcher.core.SantoralNovus
import com.foco.launcher.notification.FocoNotificationListener
import com.foco.launcher.notification.NlsStatus
import com.foco.launcher.notification.NotificationAllowlistStore
import com.foco.launcher.notification.NotificationPolicyEngine
import com.foco.launcher.notification.Silence
import com.foco.launcher.registry.GroupMutations
import com.foco.launcher.registry.GroupSection
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.security.PinStore
import com.foco.launcher.registry.PrefsStore
import com.foco.launcher.work.WorkCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FocoApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var prefsStore: PrefsStore
        private set
    lateinit var registry: PackageRegistry
        private set
    lateinit var workCatalog: WorkCatalog
        private set
    lateinit var notificationPolicy: NotificationPolicyEngine
        private set
    lateinit var pinStore: PinStore
        private set

    private val _startupReady = MutableStateFlow(false)

    /** True after the legacy notification prefs migration has finished (or failed open). */
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        prefsStore = PrefsStore(this)
        registry = PackageRegistry(this, prefsStore)
        workCatalog = WorkCatalog(this)
        notificationPolicy = NotificationPolicyEngine(this, prefsStore)
        pinStore = PinStore(this)
        applicationScope.launch(Dispatchers.Default) {
            Santoral1962.load(this@FocoApp)
            SantoralNovus.load(this@FocoApp)
            DietPlan.load(this@FocoApp)
            HomeGlance.refresh(this@FocoApp)
        }
        applicationScope.launch {
            try {
                NotificationAllowlistStore.migrateInto(
                    prefsStore,
                    NotificationAllowlistStore(this@FocoApp),
                )
            } finally {
                _startupReady.value = true
            }
            // Update sideload keeps the grant and drops the bind. Rebind, then scrub.
            NlsStatus.requestRebind(this@FocoApp)
            FocoNotificationListener.scrubIfConnected()
        }
        applicationScope.launch {
            combine(prefsStore.prefs, FocoNotificationListener.connected) { prefs, connected ->
                prefs to connected
            }.collect { (_, connected) ->
                val ready = connected && NlsStatus.isGranted(this@FocoApp)
                prefsStore.update { current ->
                    val aligned = Silence.align(
                        current.notificationsPaused,
                        current.phonePaused,
                    ).first
                    val turnOn = ready && current.silenceArmOnConnect
                    val on = aligned || turnOn
                    if (current.notificationsPaused == on &&
                        current.phonePaused == on &&
                        current.silenceArmOnConnect == (current.silenceArmOnConnect && !turnOn)
                    ) {
                        return@update current
                    }
                    current.copy(
                        notificationsPaused = on,
                        phonePaused = on,
                        silenceArmOnConnect = current.silenceArmOnConnect && !turnOn,
                    )
                }
            }
        }
        applicationScope.launch {
            workCatalog.state.collect { work ->
                if (!work.loaded || work.loadFailed) return@collect
                val live = if (work.hasWorkProfile) work.apps.map { it.key }.toSet() else emptySet()
                prefsStore.update { prefs ->
                    val next = GroupMutations.retain(prefs.groups, GroupSection.WORK, live)
                    if (next == prefs.groups) prefs else prefs.copy(groups = next)
                }
            }
        }
    }
}
