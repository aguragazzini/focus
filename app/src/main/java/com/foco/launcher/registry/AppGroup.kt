package com.foco.launcher.registry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One home section. A group never mixes the two: personal apps and work-profile
 * apps belong to different Android users.
 */
@Serializable
enum class GroupSection {
    @SerialName("personal")
    PERSONAL,

    @SerialName("work")
    WORK,
}

/**
 * Folder of apps inside a single [section].
 *
 * [members] are personal package names or work-catalog keys, in insertion order.
 * Storage is the existing launcher DataStore document ([LauncherPrefs.groups]).
 */
@Serializable
data class AppGroup(
    val id: String,
    val section: GroupSection,
    val name: String,
    val order: Int,
    val members: List<String> = emptyList(),
)
