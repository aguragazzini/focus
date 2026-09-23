package com.foco.launcher.core

/**
 * Launchpad presentation that does not touch Android. Home and settings share these.
 */
object LaunchpadRules {
    fun workPresence(
        loaded: Boolean,
        hasWorkProfile: Boolean,
        quietEnabled: Boolean?,
        loadFailed: Boolean,
    ): WorkPresence {
        if (!loaded) return WorkPresence.Unknown
        if (quietEnabled == true) return WorkPresence.Paused
        if (hasWorkProfile) return WorkPresence.Yes
        if (loadFailed) return WorkPresence.Unknown
        return WorkPresence.No
    }

    /** Huella stays off the strip until the biometric launch path actually runs. */
    fun showBioCell(launchPathEnabled: Boolean): Boolean = launchPathEnabled

    /**
     * Anti-brick: confirm when the package is the one that resolves system Settings.
     * Callers pass that resolved package; this does not hardcode com.android.settings.
     */
    fun needsSettingsConfirm(packageName: String, settingsPackage: String?): Boolean {
        return !settingsPackage.isNullOrBlank() && packageName == settingsPackage
    }

    /** OEM work-settings control exists only when the intent resolves and a profile is present. */
    fun showWorkSettingsLink(resolves: Boolean, hasWorkProfile: Boolean): Boolean {
        return resolves && hasWorkProfile
    }

    fun joinStatus(parts: List<String>): String = parts.joinToString(separator = " · ")
}

enum class WorkPresence {
    Unknown,
    Yes,
    No,
    Paused,
}
