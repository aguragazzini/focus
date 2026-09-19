package com.foco.launcher.registry

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foco.launcher.FocoApp

/**
 * PACKAGE_ADDED / REMOVED / REPLACED → invalidate icon/label cache.
 * Never auto-adds a newly installed app to the whitelist.
 * Actual uninstalls drop the orphan entry.
 */
class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val app = context.applicationContext as? FocoApp ?: return
        val pkg = intent.data?.schemeSpecificPart
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        app.registry.onPackageEvent(intent.action, pkg, replacing)
    }
}
