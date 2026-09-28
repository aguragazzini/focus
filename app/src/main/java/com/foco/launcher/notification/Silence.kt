package com.foco.launcher.notification

/**
 * One silence control over the two listener flags.
 *
 * [TODO] is the stronger cancel, so it wins when the phone pause was already on.
 * Reading a level does not write. The only full unmute is an explicit [OFF].
 */
enum class SilenceLevel {
    OFF,
    AVISOS,
    TODO,
}

object Silence {
    fun level(notificationsPaused: Boolean, phonePaused: Boolean): SilenceLevel {
        if (phonePaused) return SilenceLevel.TODO
        if (notificationsPaused) return SilenceLevel.AVISOS
        return SilenceLevel.OFF
    }

    fun notificationsPaused(level: SilenceLevel): Boolean = level != SilenceLevel.OFF

    fun phonePaused(level: SilenceLevel): Boolean = level == SilenceLevel.TODO
}
