package com.foco.launcher.registry

import kotlinx.serialization.Serializable

@Serializable
data class WhitelistEntry(
    val packageName: String,
    val order: Int,
    val bioEnabled: Boolean = true,
)

@Serializable
data class LauncherPrefs(
    val setupDone: Boolean = false,
    val biometricGlobalEnabled: Boolean = true,
    val entries: List<WhitelistEntry> = emptyList(),
)
