package com.foco.launcher.notification

/**
 * One pill over the two listener flags.
 *
 * Cycle: Sin pausa → Foco → Todo en pausa → Sin pausa.
 * [TODO] wins when the phone pause was already on, so opening the app does not unmute.
 * Reading a level does not write. [OFF] is the only full unmute, and only after a tap.
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

    fun next(level: SilenceLevel): SilenceLevel = when (level) {
        SilenceLevel.OFF -> SilenceLevel.AVISOS
        SilenceLevel.AVISOS -> SilenceLevel.TODO
        SilenceLevel.TODO -> SilenceLevel.OFF
    }

    fun notificationsPaused(level: SilenceLevel): Boolean = level != SilenceLevel.OFF

    fun phonePaused(level: SilenceLevel): Boolean = level == SilenceLevel.TODO
}
