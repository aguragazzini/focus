package com.foco.launcher.core

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.CalendarContract
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

    /**
     * Anti-brick escape. Always the system settings list.
     * A package launch intent can land on a shell that is not that list.
     */
    fun openSystemSettings(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_SETTINGS).addCategory(Intent.CATEGORY_DEFAULT)
        return ResolvedStart.start(context, intent)
    }

    /**
     * All-apps list. On some Android 13+ paths the overflow for restricted
     * settings shows here and not on the app-info screen Foco opens directly.
     */
    fun openAllApps(context: Context): Boolean {
        val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            .addCategory(Intent.CATEGORY_DEFAULT)
        return ResolvedStart.start(context, intent)
    }

    /** App info. Foco cannot add the system overflow menu to this screen. */
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

    /**
     * Opens the event, then the civil day, then the calendar app.
     * A work event tries the managed-profile viewer first, then the work calendar app.
     */
    fun openAgendaEvent(
        context: Context,
        eventId: Long,
        begin: Long,
        end: Long,
        allDay: Boolean,
        work: Boolean,
    ): Boolean {
        if (work) {
            if (Build.VERSION.SDK_INT >= 29) {
                val opened = runCatching {
                    CalendarContract.startViewCalendarEventInManagedProfile(
                        context,
                        eventId,
                        begin,
                        end,
                        allDay,
                        0,
                    )
                }.getOrDefault(false)
                if (opened) return true
            }
            if (openWorkCalendar(context)) return true
        }
        val eventUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val view = Intent(Intent.ACTION_VIEW).setData(eventUri).apply {
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, allDay)
        }
        if (ResolvedStart.start(context, view)) return true
        val day = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
        ContentUris.appendId(day, begin)
        if (ResolvedStart.start(context, Intent(Intent.ACTION_VIEW).setData(day.build()))) return true
        return if (work) false else openCalendar(context)
    }

    /** Calendar app inside the work profile. Fails soft when none resolves. */
    fun openWorkCalendar(context: Context): Boolean {
        val apps = context.getSystemService(LauncherApps::class.java) ?: return false
        val users = runCatching {
            apps.profiles.filter { it != Process.myUserHandle() }
        }.getOrDefault(emptyList())
        if (users.isEmpty()) return false
        val probe = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR)
        for (user in users) {
            val info = runCatching { apps.resolveActivity(probe, user) }.getOrNull()
            if (info != null && startWork(apps, info.componentName, user)) return true
        }
        for (pkg in listOf("com.google.android.calendar", "com.android.calendar")) {
            for (user in users) {
                val info = runCatching { apps.getActivityList(pkg, user).firstOrNull() }.getOrNull() ?: continue
                if (startWork(apps, info.componentName, user)) return true
            }
        }
        return false
    }

    private fun startWork(apps: LauncherApps, component: ComponentName, user: UserHandle): Boolean {
        return runCatching {
            apps.startMainActivity(component, user, null, null)
            true
        }.getOrDefault(false)
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
