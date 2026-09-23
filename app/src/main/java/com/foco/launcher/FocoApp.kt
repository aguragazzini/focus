package com.foco.launcher

import android.app.Application
import com.foco.launcher.notification.NotificationAllowlistStore
import com.foco.launcher.notification.NotificationPolicyEngine
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.registry.PrefsStore
import com.foco.launcher.work.WorkCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _startupReady = MutableStateFlow(false)

    /** True after the legacy notification prefs migration has finished (or failed open). */
    val startupReady: StateFlow<Boolean> = _startupReady.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        prefsStore = PrefsStore(this)
        registry = PackageRegistry(this, prefsStore)
        workCatalog = WorkCatalog(this)
        notificationPolicy = NotificationPolicyEngine(this, prefsStore)
        applicationScope.launch {
            try {
                NotificationAllowlistStore.migrateInto(
                    prefsStore,
                    NotificationAllowlistStore(this@FocoApp),
                )
            } finally {
                _startupReady.value = true
            }
        }
    }
}
