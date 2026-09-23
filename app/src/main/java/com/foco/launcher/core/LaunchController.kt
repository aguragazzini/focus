package com.foco.launcher.core

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.provider.Settings
import com.foco.launcher.registry.PackageRegistry
import com.foco.launcher.security.BiometricGate
import com.foco.launcher.work.WorkApp
import com.foco.launcher.work.WorkSettingsLink

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

    /** Work-profile launch. No Foco biometric gate. */
    fun openWorkApp(context: Context, app: WorkApp): Boolean {
        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return false
        return try {
            launcherApps.startMainActivity(app.component, app.user, null, null)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSystemSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return startSafely(context, intent)
    }

    fun workProfileSettingsResolves(context: Context): Boolean {
        return WorkSettingsLink.shouldOffer(resolveWorkSettings(context))
    }

    /**
     * Opens managed-profile settings only when that activity resolves.
     * Never falls through to a generic Settings screen (that control already exists).
     */
    fun openWorkProfileSettings(context: Context): Boolean {
        if (!resolveWorkSettings(context)) return false
        return startSafely(context, WorkSettingsLink.intent())
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

    private fun resolveWorkSettings(context: Context): Boolean {
        val resolved = context.packageManager.resolveActivity(
            WorkSettingsLink.intent(),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolved != null
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
