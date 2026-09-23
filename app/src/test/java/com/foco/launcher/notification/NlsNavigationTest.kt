package com.foco.launcher.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NlsNavigationTest {
    @Test
    fun detailPrecedesListAndGenericOnApi30() {
        val steps = NlsSettingsPlan.candidates(31, "com.foco.launcher/com.foco.launcher.notification.FocoNotificationListener")
        assertEquals(
            listOf(NlsSettingsPlan.Kind.Detail, NlsSettingsPlan.Kind.List, NlsSettingsPlan.Kind.Generic),
            steps.map { it.kind },
        )
        assertEquals(NlsSettingsPlan.ACTION_DETAIL, steps[0].action)
        assertEquals(NlsSettingsPlan.EXTRA_COMPONENT, "android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME")
        assertEquals(steps[0].componentFlat, "com.foco.launcher/com.foco.launcher.notification.FocoNotificationListener")
        assertTrue(steps[1].componentFlat == null)
    }

    @Test
    fun preApi30SkipsTheDetailScreen() {
        val steps = NlsSettingsPlan.candidates(26, "com.foco.launcher/.notification.FocoNotificationListener")
        assertEquals(listOf(NlsSettingsPlan.Kind.List, NlsSettingsPlan.Kind.Generic), steps.map { it.kind })
    }

    @Test
    fun secureSettingMatchesFlattenedAndShortNames() {
        val pkg = "com.foco.launcher"
        val cls = "com.foco.launcher.notification.FocoNotificationListener"
        val full = "$pkg/$cls"
        val short = "$pkg/.notification.FocoNotificationListener"
        assertTrue(NlsGrantMatch.listed("com.other/$cls:$full", pkg, cls))
        assertTrue(NlsGrantMatch.listed(short, pkg, cls))
        assertFalse(NlsGrantMatch.listed("com.other.app/.Listener", pkg, cls))
        assertFalse(NlsGrantMatch.listed(null, pkg, cls))
        assertFalse(NlsGrantMatch.listed("", pkg, cls))
    }

    @Test
    fun rebindIsThrottled() {
        assertFalse(NlsRebindGate.allow(nowElapsedMs = 1_000L, lastElapsedMs = 0L))
        assertTrue(NlsRebindGate.allow(nowElapsedMs = 1_500L, lastElapsedMs = 0L))
        assertTrue(NlsRebindGate.allow(nowElapsedMs = 4_000L, lastElapsedMs = 2_000L))
    }

    @Test
    fun manifestDeclaresListenerQueriesAndSettingsLeavesTheHomeTask() {
        val xml = File("src/main/AndroidManifest.xml").readText()
        assertTrue(xml.contains("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        assertTrue(xml.contains("android.settings.NOTIFICATION_LISTENER_DETAIL_SETTINGS"))
        assertTrue(xml.contains("android.settings.APPLICATION_DETAILS_SETTINGS"))
        assertTrue(xml.contains("android.intent.action.SHOW_ALARMS"))
        assertTrue(xml.contains("android.intent.category.APP_CALENDAR"))
        val settings = xml.substringAfter(".settings.SettingsActivity").substringBefore("</activity>")
        assertTrue(settings.contains("com.foco.launcher.settings"))
        assertTrue(settings.contains("singleTop"))
        val activity = File("src/main/java/com/foco/launcher/settings/SettingsActivity.kt").readText()
        assertTrue(activity.contains("FLAG_ACTIVITY_NEW_TASK"))
        assertTrue(activity.contains("FLAG_ACTIVITY_CLEAR_TOP"))
        val listener = File("src/main/java/com/foco/launcher/notification/FocoNotificationListener.kt").readText()
        assertTrue(listener.contains("requestRebind"))
        assertFalse(listener.contains("DevicePolicyManager"))
        val launcher = File("src/main/java/com/foco/launcher/core/LauncherActivity.kt").readText()
        assertTrue(launcher.contains("needsSettingsConfirm"))
        assertTrue(launcher.contains("openSystemSettings"))
        assertTrue(launcher.contains("openClock"))
        assertTrue(launcher.contains("openCalendar"))
    }
}
