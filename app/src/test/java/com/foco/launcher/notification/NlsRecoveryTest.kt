package com.foco.launcher.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NlsRecoveryTest {
    @Test
    fun reinstallWithoutGrantNeedsAttentionEvenWhenTheToggleIsOff() {
        assertEquals(
            NlsRecovery.Attention.Grant,
            NlsRecovery.attention(granted = false, filterEnabled = false, connected = false),
        )
        assertFalse(NlsRecovery.filterIsActive(granted = false, filterEnabled = false, connected = false))
    }

    @Test
    fun disconnectedListenerMattersOnlyAfterTheToggleIsOn() {
        assertEquals(
            NlsRecovery.Attention.Disconnected,
            NlsRecovery.attention(granted = true, filterEnabled = true, connected = false),
        )
        assertEquals(
            NlsRecovery.Attention.None,
            NlsRecovery.attention(granted = true, filterEnabled = false, connected = false),
        )
    }

    @Test
    fun activeRequiresGrantToggleAndALiveBinding() {
        assertTrue(NlsRecovery.filterIsActive(granted = true, filterEnabled = true, connected = true))
        assertFalse(NlsRecovery.filterIsActive(granted = true, filterEnabled = true, connected = false))
        assertFalse(NlsRecovery.filterIsActive(granted = true, filterEnabled = false, connected = true))
        assertFalse(NlsRecovery.filterIsActive(granted = false, filterEnabled = true, connected = true))
    }

    @Test
    fun copyNamesTheRestrictedSettingsPathAndDoesNotClaimActiveEarly() {
        val xml = File("src/main/res/values/strings.xml").readText()
        assertTrue(xml.contains("Permitir ajustes restringidos"))
        assertTrue(xml.contains("Acceso a notificaciones"))
        assertTrue(xml.contains("Silenciar otras notificaciones"))
        assertTrue(xml.contains("Abrir info de la app"))
        val screens = File("src/main/java/com/foco/launcher/settings/NotificationSettingsScreens.kt").readText()
        assertTrue(screens.contains("nls_filter_status_active"))
        assertTrue(screens.contains("nlsActive"))
        val launch = File("src/main/java/com/foco/launcher/core/LaunchController.kt").readText()
        val escape = launch.substringAfter("fun openSystemSettings").substringBefore("fun openClock")
        assertTrue(escape.contains("ACTION_SETTINGS"))
        assertFalse(escape.contains("getLaunchIntentForPackage"))
    }
}
