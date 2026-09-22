package com.foco.launcher.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foco.launcher.FocoApp
import kotlinx.coroutines.launch

/**
 * Starts the process after reboot and warms whitelist icons off the main thread.
 * Does not query every installed app.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? FocoApp ?: return
        val pending = goAsync()
        app.applicationScope.launch {
            try {
                app.registry.warmHomeIcons()
            } finally {
                pending.finish()
            }
        }
    }
}
