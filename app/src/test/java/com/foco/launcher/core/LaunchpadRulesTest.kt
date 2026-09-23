package com.foco.launcher.core

import com.foco.launcher.security.BiometricGate
import com.foco.launcher.work.WorkSettingsLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchpadRulesTest {
    @Test
    fun stripWorkCellUsesQuietOnlyWhenTheApiSaysSo() {
        assertEquals(WorkPresence.Unknown, LaunchpadRules.workPresence(false, true, null, false))
        assertEquals(WorkPresence.No, LaunchpadRules.workPresence(true, false, null, false))
        assertEquals(WorkPresence.Yes, LaunchpadRules.workPresence(true, true, null, false))
        assertEquals(WorkPresence.Yes, LaunchpadRules.workPresence(true, true, false, false))
        assertEquals(WorkPresence.Paused, LaunchpadRules.workPresence(true, true, true, false))
        assertEquals(WorkPresence.Unknown, LaunchpadRules.workPresence(true, false, null, true))
    }

    @Test
    fun bioCellStaysHiddenWhileLaunchPathIsOff() {
        assertFalse(BiometricGate.ENABLED_IN_LAUNCH_PATH)
        assertFalse(LaunchpadRules.showBioCell(BiometricGate.ENABLED_IN_LAUNCH_PATH))
        assertTrue(LaunchpadRules.showBioCell(true))
    }

    @Test
    fun settingsRemovalConfirmsTheResolvedPackageOnly() {
        assertFalse(LaunchpadRules.needsSettingsConfirm("com.android.settings", null))
        assertFalse(LaunchpadRules.needsSettingsConfirm("com.android.settings", "com.motorola.settings"))
        assertTrue(LaunchpadRules.needsSettingsConfirm("com.motorola.settings", "com.motorola.settings"))
        assertFalse(LaunchpadRules.needsSettingsConfirm("com.motorola.settings", " "))
    }

    @Test
    fun workSettingsLinkIsOmittedUnlessItResolves() {
        assertFalse(WorkSettingsLink.shouldOffer(false))
        assertTrue(WorkSettingsLink.shouldOffer(true))
        assertEquals("android.settings.MANAGED_PROFILE_SETTINGS", WorkSettingsLink.ACTION)
        assertFalse(LaunchpadRules.showWorkSettingsLink(resolves = false, hasWorkProfile = true))
        assertFalse(LaunchpadRules.showWorkSettingsLink(resolves = true, hasWorkProfile = false))
        assertTrue(LaunchpadRules.showWorkSettingsLink(resolves = true, hasWorkProfile = true))
    }

    @Test
    fun statusPartsJoinWithAMiddleDot() {
        assertEquals(
            "Personal 8 · Filtro Off · Trabajo Sí",
            LaunchpadRules.joinStatus(listOf("Personal 8", "Filtro Off", "Trabajo Sí")),
        )
    }
}
