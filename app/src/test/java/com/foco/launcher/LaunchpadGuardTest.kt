package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LaunchpadGuardTest {
    @Test
    fun workListDoesNotDecodeIcons() {
        val src = File("src/main/java/com/foco/launcher/work/WorkCatalog.kt").readText()
        val query = src.substringAfter("private fun query(").substringBefore("private fun toWorkApp")
        assertFalse(query.contains("getIcon"))
        assertFalse(query.contains("toBitmapCached"))
        assertTrue(src.contains("fun ensureIcon"))
        val quiet = File("src/main/java/com/foco/launcher/work/QuietMode.kt").readText()
        assertTrue(quiet.contains("isQuietModeEnabled"))
        assertFalse(quiet.contains("requestQuietModeEnabled"))
    }

    @Test
    fun launcherDoesNotPauseOrAdministerWork() {
        val root = File("src/main/java")
        val banned = listOf(
            "DevicePolicyManager",
            "requestQuietModeEnabled",
            "QUERY_ALL_PACKAGES",
            "setInterruptionFilter",
        )
        val hits = root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .flatMap { file ->
                val text = file.readText()
                banned.filter { text.contains(it) }.map { "${file.name}:$it" }
            }
            .toList()
        assertTrue(hits.toString(), hits.isEmpty())
    }

    @Test
    fun homeChromeStaysOverflowOnly() {
        val src = File("src/main/java/com/foco/launcher/core/HomeScreen.kt").readText()
        assertFalse(src.contains("TopAppBar"))
        assertFalse(src.contains("Icons.Outlined.Settings"))
        assertTrue(src.contains("MoreVert"))
        assertTrue(src.contains("menu_refresh_work"))
        assertTrue(src.contains("lp_remove"))
        assertTrue(src.contains("lp_open"))
    }

    @Test
    fun homeIsClockThenPersonalThenWorkWithoutStripChrome() {
        val src = File("src/main/java/com/foco/launcher/core/HomeScreen.kt").readText()
        val clock = src.indexOf("HomeClock")
        val personal = src.indexOf("section_personal")
        val work = src.indexOf("section_work")
        assertTrue(clock >= 0 && personal > clock && work > personal)
        assertFalse(src.contains("StatusStrip"))
        assertFalse(src.contains("work_sub"))
        assertFalse(src.contains("personal_edit"))
        assertFalse(src.contains("Icons.Outlined.Refresh"))
        assertFalse(src.contains("AppWidgetHost"))
        assertFalse(src.contains("Santoral"))
        assertFalse(src.contains("open-meteo"))
        assertTrue(src.contains("home_empty"))
        assertTrue(src.contains("home_add"))
        val clockSrc = File("src/main/java/com/foco/launcher/core/HomeClockFormat.kt").readText()
        assertFalse(clockSrc.contains("http"))
        assertFalse(clockSrc.contains("weather"))
        assertFalse(clockSrc.contains("AppWidgetHost"))
    }
}
