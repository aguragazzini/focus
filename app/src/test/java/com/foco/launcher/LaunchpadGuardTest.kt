package com.foco.launcher

import com.foco.launcher.core.HOME_LANDING_PAGE
import com.foco.launcher.core.HOME_PAGE_CLOCK
import com.foco.launcher.core.HOME_PAGE_COUNT
import com.foco.launcher.core.HOME_PAGE_DIET
import com.foco.launcher.core.HOME_PAGE_PERSONAL
import com.foco.launcher.core.HOME_PAGE_WORK
import org.junit.Assert.assertEquals
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
        assertTrue(src.contains("santoral_1962.json"))
        assertTrue(src.contains("SantoralLine"))
        assertFalse(src.contains("open-meteo"))
        assertFalse(src.contains("LocalTemperature"))
        assertTrue(src.contains("home_empty"))
        assertTrue(src.contains("home_add"))
        assertTrue(src.contains("HorizontalPager"))
        assertTrue(src.contains("santoral_novus.json"))
        assertTrue(src.contains("plan_ragazzini.json"))
        assertEquals(0, HOME_PAGE_CLOCK)
        assertEquals(1, HOME_PAGE_PERSONAL)
        assertEquals(2, HOME_PAGE_DIET)
        assertEquals(3, HOME_PAGE_WORK)
        assertEquals(4, HOME_PAGE_COUNT)
        assertEquals(HOME_PAGE_PERSONAL, HOME_LANDING_PAGE)
        assertTrue(src.contains("initialPage = HOME_LANDING_PAGE"))
        val clockFn = src.substringAfter("fun ClockHomePage").substringBefore("fun PersonalHomePage")
        assertTrue(clockFn.contains("HomeClock"))
        assertTrue(clockFn.contains("santoral_1962.json"))
        assertTrue(clockFn.contains("santoral_novus.json"))
        assertTrue(clockFn.contains("SantoralLine"))
        assertFalse(clockFn.contains("plan_ragazzini"))
        assertFalse(clockFn.contains("DietPage"))
        assertFalse(clockFn.contains("nls_pause"))
        assertFalse(clockFn.contains("work_pause"))
        val personalFn = src.substringAfter("fun PersonalHomePage").substringBefore("fun WorkHomePage")
        assertTrue(personalFn.contains("section_personal"))
        assertTrue(personalFn.contains("nls_pause"))
        assertFalse(personalFn.contains("section_work"))
        assertFalse(personalFn.contains("work_pause"))
        val workFn = src.substringAfter("fun WorkHomePage").substringBefore("fun HomePagerCue")
        assertTrue(workFn.contains("section_work"))
        assertTrue(workFn.contains("work_pause"))
        assertFalse(workFn.contains("nls_pause"))
        val calls = src.substringBefore("fun ClockHomePage")
        val clockCall = calls.indexOf("ClockHomePage(")
        val personalCall = calls.indexOf("PersonalHomePage(")
        val dietCall = calls.indexOf("DietPage(")
        val workCall = calls.indexOf("WorkHomePage(")
        assertTrue(clockCall >= 0 && personalCall > clockCall && dietCall > personalCall && workCall > dietCall)
        val clockUi = File("src/main/java/com/foco/launcher/core/HomeClock.kt").readText()
        val underDate = clockUi.indexOf("underDate(day)")
        val glanceText = clockUi.indexOf("text = glance")
        assertTrue(underDate >= 0 && glanceText > underDate)
        assertFalse(clockUi.contains("http"))
        assertFalse(clockUi.contains("weather"))
        val clockSrc = File("src/main/java/com/foco/launcher/core/HomeClockFormat.kt").readText()
        assertFalse(clockSrc.contains("http"))
        assertFalse(clockSrc.contains("weather"))
        assertFalse(clockSrc.contains("AppWidgetHost"))
    }
}
