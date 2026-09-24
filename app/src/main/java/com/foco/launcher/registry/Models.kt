package com.foco.launcher.registry

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistEntry(
    val packageName: String,
    val order: Int,
    val bioEnabled: Boolean = false,
    val allowNotif: Boolean = true,
)

@Serializable
data class LauncherPrefs(
    val setupDone: Boolean = false,
    val biometricGlobalEnabled: Boolean = false,
    val entries: List<WhitelistEntry> = emptyList(),
    val nlsFilterEnabled: Boolean = false,
    /** Home folders. Absent on v0.5.1 documents; decode falls back to empty. */
    val groups: List<AppGroup> = emptyList(),
) {
    /** Andrés: personal packages that may notify. Derived from home + allowNotif. */
    val notificationAllowlist: Set<String>
        get() = entries.filter { it.allowNotif }.map { it.packageName }.toSet()
}
