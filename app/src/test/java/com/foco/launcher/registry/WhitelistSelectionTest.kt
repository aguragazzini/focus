package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistSelectionTest {
    @Test
    fun continueForcesSettingsOntoEmptySelection() {
        assertEquals(
            setOf("com.android.settings"),
            WhitelistSelection.withSettings(emptySet(), "com.android.settings"),
        )
    }

    @Test
    fun continueKeepsEssentialsAndAddsSettings() {
        assertEquals(
            setOf("com.google.android.dialer", "com.android.settings"),
            WhitelistSelection.withSettings(setOf("com.google.android.dialer"), "com.android.settings"),
        )
    }

    @Test
    fun emptyStaysEmptyWhenSettingsDidNotResolve() {
        assertTrue(WhitelistSelection.withSettings(emptySet(), null).isEmpty())
    }
}
