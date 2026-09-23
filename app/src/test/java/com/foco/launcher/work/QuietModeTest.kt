package com.foco.launcher.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuietModeTest {
    @Test
    fun onlyASuccessfulReadBecomesPausedOrNot() {
        assertEquals(true, QuietMode.interpret(Result.success(true)))
        assertEquals(false, QuietMode.interpret(Result.success(false)))
    }

    @Test
    fun securityFailureIsUnknownNotPaused() {
        assertNull(QuietMode.interpret(Result.failure(SecurityException("not allowed"))))
        assertNull(QuietMode.interpret(Result.failure(IllegalStateException("missing"))))
    }
}
