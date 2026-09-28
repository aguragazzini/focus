package com.foco.launcher.core

/**
 * Coming back to the launcher resets the pager to Personal and leaves page edit.
 * An edit-home or open-page intent must not reset: [android.app.Activity.onRestart]
 * runs before [android.app.Activity.onNewIntent], so the decision belongs on resume,
 * once that intent is the current one.
 */
internal object HomeReturn {
    fun resets(editRequested: Boolean, openPage: String?): Boolean {
        if (editRequested) return false
        if (!openPage.isNullOrBlank()) return false
        return true
    }
}
