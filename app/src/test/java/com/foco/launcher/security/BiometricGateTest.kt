package com.foco.launcher.security

import org.junit.Assert.assertFalse
import org.junit.Test

class BiometricGateTest {
    @Test
    fun launchPathOffKeepsBioDefaultsOff() {
        assertFalse(BiometricGate.ENABLED_IN_LAUNCH_PATH)
        assertFalse(BiometricGate.defaultBioEnabled(isPhone = true))
        assertFalse(BiometricGate.defaultBioEnabled(isPhone = false))
    }
}
