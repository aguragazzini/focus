package com.foco.launcher.registry

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistEntry(
    val packageName: String,
    val order: Int,
    val bioEnabled: Boolean = true,
    val allowNotif: Boolean = true,
)

@Serializable
data class LauncherPrefs(
    val setupDone: Boolean = false,
    val biometricGlobalEnabled: Boolean = true,
    val entries: List<WhitelistEntry> = emptyList(),
    val nlsFilterEnabled: Boolean = false,
) {
    /** Andrés: personal packages that may notify. Derived from home + allowNotif. */
    val notificationAllowlist: Set<String>
        get() = entries.filter { it.allowNotif }.map { it.packageName }.toSet()
}
