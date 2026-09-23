package com.foco.launcher.core

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.security.BiometricGate

/**
 * Unique protected launch path: taps from Foco home.
 * BiometricPrompt stays off until [BiometricGate.ENABLED_IN_LAUNCH_PATH] is true.
 */
object LaunchController {
    fun openApp(context: Context, registry: PackageRegistry, packageName: String): Boolean {
        if (BiometricGate.ENABLED_IN_LAUNCH_PATH) {
            // Prompt, then resolve. Unused while the launch path flag is false.
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

    /**
     * System default-apps screen, where the user can pick another home app.
     * This is not the same entry as [openHomePicker].
     */
    fun openDefaultAppsSettings(context: Context): Boolean {
        val defaults = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (startSafely(context, defaults)) return true
        return openHomePicker(context)
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
