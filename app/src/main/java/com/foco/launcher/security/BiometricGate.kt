package com.foco.launcher.security

/**
 * BiometricPrompt gate.
 *
 * The launch path in [com.foco.launcher.core] ignores biometrics while
 * [ENABLED_IN_LAUNCH_PATH] is false. No biometric UI is shown in that state.
 * Stored `bioEnabled` defaults stay false so prefs do not claim a prompt
 * the launcher does not run.
 */
object BiometricGate {
    const val ENABLED_IN_LAUNCH_PATH: Boolean = false

    fun defaultBioEnabled(isPhone: Boolean): Boolean {
        if (!ENABLED_IN_LAUNCH_PATH) return false
        return !isPhone
    }
}
