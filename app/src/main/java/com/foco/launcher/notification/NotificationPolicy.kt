package com.foco.launcher.notification

/**
 * Pure policy. Android types stay out so unit tests can run on the JVM.
 *
 * Spares always pass: protected system/OEM, CALL, ALARM, NAVIGATION, TRANSPORT,
 * MediaStyle, media session, dialer. Listener not granted → no cancel.
 *
 * Work profile passes unless [workSectionPaused] or [notificationsPaused].
 * Personal: [notificationsPaused] cancels in-scope apps (allowlist does not save them).
 * Otherwise the allowlist filter runs only while [nlsFilterEnabled].
 *
 * This is Foco's listener cancel path. It does not touch interruption filters,
 * notification-policy access, device policy, or system quiet mode.
 */
object NotificationPolicy {
    data class Facts(
        val packageName: String,
        val isWorkOrOtherProfile: Boolean,
        val category: String?,
        val isMediaStyle: Boolean,
        val hasMediaSession: Boolean,
        val isDialerPackage: Boolean,
    )

    const val CATEGORY_CALL = "call"
    const val CATEGORY_ALARM = "alarm"
    const val CATEGORY_NAVIGATION = "navigation"
    const val CATEGORY_TRANSPORT = "transport"

    fun shouldSuppress(
        facts: Facts,
        nlsFilterEnabled: Boolean,
        allowlist: Set<String>,
        listenerGranted: Boolean = true,
        workSectionPaused: Boolean = false,
        notificationsPaused: Boolean = false,
    ): Boolean {
        if (!listenerGranted) return false
        if (isProtectedSystemOrOem(facts.packageName)) return false
        if (isCallAlarmMediaException(facts)) return false
        if (facts.isWorkOrOtherProfile) {
            return workSectionPaused || notificationsPaused
        }
        if (notificationsPaused) return true
        if (!nlsFilterEnabled) return false
        return facts.packageName !in allowlist
    }

    fun isProtectedSystemOrOem(packageName: String): Boolean {
        if (packageName == "android" || packageName == "com.foco.launcher") return true
        if (packageName.startsWith("android.")) return true
        if (packageName.startsWith("com.android.")) return true
        if (packageName.startsWith("com.google.android.apps.security")) return true
        if (packageName == "com.google.android.permissioncontroller") return true
        if (packageName.startsWith("com.motorola.android.")) return true
        if (packageName == "com.motorola.ccc" || packageName == "com.motorola.ccc.notification") return true
        return false
    }

    fun isCallAlarmMediaException(facts: Facts): Boolean {
        if (facts.isDialerPackage) return true
        val category = facts.category
        if (category == CATEGORY_CALL ||
            category == CATEGORY_ALARM ||
            category == CATEGORY_NAVIGATION ||
            category == CATEGORY_TRANSPORT
        ) {
            return true
        }
        if (facts.isMediaStyle || facts.hasMediaSession) return true
        return false
    }
}
