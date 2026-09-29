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
    /**
     * Personal whitelist order was changed with the up/down controls.
     * Until then the home and the editor show Spanish A→Z, not insertion order.
     */
    val whitelistCustomOrder: Boolean = false,
    /** Hide the Trabajo section and cancel in-scope work notifications. Not system quiet mode. */
    val workSectionPaused: Boolean = false,
    /** Foco silence. Cancels in-scope notifications. Not system Do Not Disturb. */
    val notificationsPaused: Boolean = false,
    /** Text-first tiles. Default off so icons stay on. */
    val namesOnly: Boolean = false,
    /**
     * Cancel every notification the listener can see. Not system Do Not Disturb.
     * Unpausing stops new cancels and does not restore ones already removed.
     * With [notificationsPaused], this is the Foco side of the two-state pill.
     */
    val phonePaused: Boolean = false,
    /**
     * The pill asked for the listener. The next time it connects, Foco turns on.
     * A chosen Off is left alone until that tap.
     */
    val silenceArmOnConnect: Boolean = false,
    /** These packages do not launch, and the listener cancels their notifications. */
    val pausedPackages: List<String> = emptyList(),
    /**
     * Home pager. Empty means the default pages (Reloj, Agenda, Personal, Uso, Comida, Trabajo).
     * Unknown or all-hidden lists are restored by [HomePages.resolve], not here.
     */
    val pages: List<HomePageSpec> = emptyList(),
    /**
     * The user has created, renamed, reordered, hidden, or deleted a page.
     * Stops the 0.9.2 four-page list from gaining Agenda again after a delete.
     */
    val pageLayoutEdited: Boolean = false,
) {
    /** Andrés: personal packages that may notify. Derived from home + allowNotif. */
    val notificationAllowlist: Set<String>
        get() = entries.filter { it.allowNotif }.map { it.packageName }.toSet()
}
