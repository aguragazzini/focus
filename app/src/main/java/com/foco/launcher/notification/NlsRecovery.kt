package com.foco.launcher.notification

/**
 * When the home banner and settings may claim the personal filter is working.
 * Work notifications are never part of this decision.
 */
object NlsRecovery {
    enum class Attention { None, Grant, Disconnected }

    /**
     * After reinstall the grant is gone and the toggle defaults off, so a missing
     * grant always needs the banner. A disconnected listener only matters once the
     * user has asked the filter to run.
     */
    fun attention(granted: Boolean, filterEnabled: Boolean, connected: Boolean): Attention {
        if (!granted) return Attention.Grant
        if (filterEnabled && !connected) return Attention.Disconnected
        return Attention.None
    }

    /** Bound, granted, and the Foco toggle on. Anything less is not "activo". */
    fun filterIsActive(granted: Boolean, filterEnabled: Boolean, connected: Boolean): Boolean {
        return granted && filterEnabled && connected
    }
}
