package com.foco.launcher.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.security.BiometricGate

/**
 * Unique protected launch path in v0.1: taps from Foco home.
 * Semana 1: no BiometricPrompt. Semana 2 will branch on
 * [BiometricGate.ENABLED_IN_LAUNCH_PATH] + per-entry bioEnabled.
 */
object LaunchController {
    fun openApp(context: Context, registry: PackageRegistry, packageName: String): Boolean {
        if (BiometricGate.ENABLED_IN_LAUNCH_PATH) {
            // Semana 2: prompt then resolve. Unused in week 1.
        }
        val intent = registry.resolveLaunchIntent(packageName) ?: return false
        return startSafely(context, intent)
    }

    fun openSystemSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, intent)
    }

    fun openHomePicker(context: Context): Boolean {
        val home = Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(context, home)) return true
        val defaults = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, defaults)
    }

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(intent, 0)
        return resolve?.activityInfo?.packageName == context.packageName
    }

    private fun startSafely(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
