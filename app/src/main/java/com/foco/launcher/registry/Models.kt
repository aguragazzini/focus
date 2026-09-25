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
    /** Hide the Trabajo section and cancel in-scope work notifications. Not system quiet mode. */
    val workSectionPaused: Boolean = false,
    /** Foco silence. Cancels in-scope notifications. Not system Do Not Disturb. */
    val notificationsPaused: Boolean = false,
    /** Text-first tiles. Default off so icons stay on. */
    val namesOnly: Boolean = false,
) {
    /** Andrés: personal packages that may notify. Derived from home + allowNotif. */
    val notificationAllowlist: Set<String>
        get() = entries.filter { it.allowNotif }.map { it.packageName }.toSet()
}
