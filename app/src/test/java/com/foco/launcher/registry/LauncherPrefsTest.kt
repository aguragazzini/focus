package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherPrefsTest {
    @Test
    fun notificationAllowlistDerivesFromAllowNotif() {
        val prefs = LauncherPrefs(
            entries = listOf(
                WhitelistEntry("home.on", 0, allowNotif = true),
                WhitelistEntry("home.off", 1, allowNotif = false),
            ),
        )
        assertEquals(setOf("home.on"), prefs.notificationAllowlist)
        assertFalse("com.instagram.android" in prefs.notificationAllowlist)
        assertTrue(WhitelistEntry("new.pkg", 0).allowNotif)
        assertFalse(WhitelistEntry("new.pkg", 0).bioEnabled)
        assertFalse(LauncherPrefs().biometricGlobalEnabled)
    }

    @Test
    fun emptyHomeMeansEmptyAllowlist() {
        assertTrue(LauncherPrefs().notificationAllowlist.isEmpty())
        assertFalse(LauncherPrefs().nlsFilterEnabled)
    }
}
