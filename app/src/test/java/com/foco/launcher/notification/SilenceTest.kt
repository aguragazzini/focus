package com.foco.launcher.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceTest {
    @Test
    fun phonePauseStaysTheStrongerLevel() {
        assertEquals(SilenceLevel.TODO, Silence.level(notificationsPaused = false, phonePaused = true))
        assertEquals(SilenceLevel.TODO, Silence.level(notificationsPaused = true, phonePaused = true))
        assertTrue(Silence.phonePaused(SilenceLevel.TODO))
        assertTrue(Silence.notificationsPaused(SilenceLevel.TODO))
    }

    @Test
    fun avisosAloneStaysAvisos() {
        assertEquals(SilenceLevel.AVISOS, Silence.level(notificationsPaused = true, phonePaused = false))
        assertTrue(Silence.notificationsPaused(SilenceLevel.AVISOS))
        assertFalse(Silence.phonePaused(SilenceLevel.AVISOS))
    }

    @Test
    fun neitherFlagIsOff() {
        assertEquals(SilenceLevel.OFF, Silence.level(notificationsPaused = false, phonePaused = false))
        assertFalse(Silence.notificationsPaused(SilenceLevel.OFF))
        assertFalse(Silence.phonePaused(SilenceLevel.OFF))
    }
}
