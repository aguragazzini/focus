package com.foco.launcher.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceTest {
    @Test
    fun eitherStoredPauseReadsAsFocoAndWritesCancelAll() {
        assertEquals(SilenceLevel.FOCO, Silence.level(notificationsPaused = false, phonePaused = true))
        assertEquals(SilenceLevel.FOCO, Silence.level(notificationsPaused = true, phonePaused = true))
        assertEquals(SilenceLevel.FOCO, Silence.level(notificationsPaused = true, phonePaused = false))
        assertTrue(Silence.phonePaused(SilenceLevel.FOCO))
        assertTrue(Silence.notificationsPaused(SilenceLevel.FOCO))
    }

    @Test
    fun neitherFlagIsOff() {
        assertEquals(SilenceLevel.OFF, Silence.level(notificationsPaused = false, phonePaused = false))
        assertFalse(Silence.notificationsPaused(SilenceLevel.OFF))
        assertFalse(Silence.phonePaused(SilenceLevel.OFF))
    }

    @Test
    fun pillTogglesOffAndFoco() {
        assertEquals(SilenceLevel.FOCO, Silence.toggle(SilenceLevel.OFF))
        assertEquals(SilenceLevel.OFF, Silence.toggle(SilenceLevel.FOCO))
    }

    @Test
    fun inScopePauseAlignsToCancelAllAndOffStaysOff() {
        assertEquals(true to true, Silence.align(notificationsPaused = true, phonePaused = false))
        assertEquals(true to true, Silence.align(notificationsPaused = false, phonePaused = true))
        assertEquals(true to true, Silence.align(notificationsPaused = true, phonePaused = true))
        assertEquals(false to false, Silence.align(notificationsPaused = false, phonePaused = false))
    }
}
