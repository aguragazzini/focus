package com.foco.launcher.notification

/**
 * One pill, two states.
 *
 * Off leaves notifications alone. Foco cancels every notification the listener can see
 * (the old cancel-all flag). The in-scope filter is not a pill state.
 * Reading a level does not write. An in-scope-only store still reads as Foco so the
 * next save turns cancel-all on. [OFF] is the only unmute, and only after a tap.
 */
enum class SilenceLevel {
    OFF,
    FOCO,
}

object Silence {
    fun level(notificationsPaused: Boolean, phonePaused: Boolean): SilenceLevel {
        return if (phonePaused || notificationsPaused) SilenceLevel.FOCO else SilenceLevel.OFF
    }

    fun toggle(level: SilenceLevel): SilenceLevel = when (level) {
        SilenceLevel.OFF -> SilenceLevel.FOCO
        SilenceLevel.FOCO -> SilenceLevel.OFF
    }

    /** Foco writes both flags. [phonePaused] is what cancels spares. */
    fun notificationsPaused(level: SilenceLevel): Boolean = level == SilenceLevel.FOCO

    fun phonePaused(level: SilenceLevel): Boolean = level == SilenceLevel.FOCO

    /**
     * Stored in-scope-only pause becomes cancel-all. Off stays off.
     * Does not turn a chosen Off into On.
     */
    fun align(notificationsPaused: Boolean, phonePaused: Boolean): Pair<Boolean, Boolean> {
        val on = notificationsPaused || phonePaused
        return on to on
    }
}
