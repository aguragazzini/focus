package com.foco.launcher

import android.app.Application
import com.foco.launcher.notification.NotificationAllowlistStore
import com.foco.launcher.notification.NotificationPolicyEngine
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.registry.PrefsStore
import kotlinx.coroutines.runBlocking

class FocoApp : Application() {
    lateinit var prefsStore: PrefsStore
        private set
    lateinit var registry: PackageRegistry
        private set
    lateinit var notificationPolicy: NotificationPolicyEngine
        private set

    override fun onCreate() {
        super.onCreate()
        prefsStore = PrefsStore(this)
        runBlocking {
            NotificationAllowlistStore.migrateInto(prefsStore, NotificationAllowlistStore(this@FocoApp))
        }
        registry = PackageRegistry(this, prefsStore)
        notificationPolicy = NotificationPolicyEngine(this, prefsStore)
    }
}
