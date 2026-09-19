package com.foco.launcher

import android.app.Application
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.registry.PrefsStore

class FocoApp : Application() {
    lateinit var prefsStore: PrefsStore
        private set
    lateinit var registry: PackageRegistry
        private set

    override fun onCreate() {
        super.onCreate()
        prefsStore = PrefsStore(this)
        registry = PackageRegistry(this, prefsStore)
    }
}
