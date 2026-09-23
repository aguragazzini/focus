package com.foco.launcher.work

import android.content.Intent

/**
 * Platform action for managed-profile settings.
 * AOSP documents it as `Settings.ACTION_MANAGED_PROFILE_SETTINGS` (@hide):
 * "In some cases, a matching Activity may not exist, so ensure you safeguard against this."
 * Offer the control only when PackageManager resolves this intent.
 */
object WorkSettingsLink {
    const val ACTION = "android.settings.MANAGED_PROFILE_SETTINGS"

    fun intent(): Intent {
        return Intent(ACTION)
            .addCategory(Intent.CATEGORY_DEFAULT)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun shouldOffer(resolved: Boolean): Boolean = resolved
}
