package com.foco.launcher.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationAllowlistPrefs(
    val nlsFilterEnabled: Boolean = false,
    val packages: Set<String> = emptySet(),
    val seededFromHome: Boolean = false,
)
