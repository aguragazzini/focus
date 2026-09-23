package com.foco.launcher.core

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.provider.Settings
import android.net.Uri
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

    /**
     * Anti-brick escape. Always the system settings list.
     * A package launch intent can land on a shell that is not that list.
     */
    fun openSystemSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS).addCategory(Intent.CATEGORY_DEFAULT)
        return ResolvedStart.start(context, intent)
    }

    /** App info, where Android 13+ hides "Permitir ajustes restringidos" for a sideload. */
    fun openAppDetails(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .setData(Uri.fromParts("package", context.packageName, null))
        return ResolvedStart.start(context, intent)
    }

    /** Clock app if one resolves. No-op when the device has none. No weather. */
    fun openClock(context: Context): Boolean = openLink(context, SystemAppLinks.clockCandidates())

    /** Calendar app if one resolves. No-op when the device has none. */
    fun openCalendar(context: Context): Boolean = openLink(context, SystemAppLinks.calendarCandidates())

    fun workProfileSettingsResolves(context: Context): Boolean {
        return WorkSettingsLink.shouldOffer(resolveWorkSettings(context))
    }

    /**
     * Opens managed-profile settings only when that activity resolves.
     * Never falls through to a generic Settings screen (that control already exists).
     */
    fun openWorkProfileSettings(context: Context): Boolean {
        if (!resolveWorkSettings(context)) return false
        return ResolvedStart.start(context, WorkSettingsLink.intent())
    }

    fun openHomePicker(context: Context): Boolean {
        if (ResolvedStart.start(context, Intent(Settings.ACTION_HOME_SETTINGS))) return true
        return ResolvedStart.start(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    }

    /**
     * System default-apps screen, where the user can pick another home app.
     * This is not the same entry as [openHomePicker].
     */
    fun openDefaultAppsSettings(context: Context): Boolean {
        if (ResolvedStart.start(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) return true
        return openHomePicker(context)
    }

    fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(intent, 0)
        return resolve?.activityInfo?.packageName == context.packageName
    }

    private fun resolveWorkSettings(context: Context): Boolean {
        return runCatching {
            context.packageManager.resolveActivity(
                WorkSettingsLink.intent(),
                PackageManager.MATCH_DEFAULT_ONLY,
            ) != null
        }.getOrDefault(false)
    }

    private fun openLink(context: Context, candidates: List<SystemAppLinks.Candidate>): Boolean {
        for (candidate in candidates) {
            val intent = linkIntent(context, candidate) ?: continue
            if (ResolvedStart.start(context, intent)) return true
        }
        return false
    }

    private fun linkIntent(context: Context, candidate: SystemAppLinks.Candidate): Intent? {
        val pkg = candidate.launcherPackage
        if (pkg != null) {
            return context.packageManager.getLaunchIntentForPackage(pkg)
        }
        return Intent(candidate.action).apply {
            candidate.categories.forEach { addCategory(it) }
        }
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
