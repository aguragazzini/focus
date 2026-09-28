package com.foco.launcher

import com.foco.launcher.registry.HomePages
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
        val calls = src.substringBefore("fun ClockHomePage")
        val clock = calls.indexOf("ClockHomePage(")
        val personal = calls.indexOf("PersonalHomePage(")
        val work = calls.indexOf("WorkHomePage(")
        assertTrue(clock >= 0 && personal > clock && work > personal)
        assertFalse(src.contains("StatusStrip"))
        assertFalse(src.contains("work_sub"))
        assertFalse(src.contains("personal_edit"))
        assertFalse(src.contains("Icons.Outlined.Refresh"))
        assertFalse(src.contains("AppWidgetHost"))
        val blockUi = File("src/main/java/com/foco/launcher/core/PageBlocksUi.kt").readText()
        assertTrue(blockUi.contains("santoral_1962.json"))
        assertTrue(blockUi.contains("SantoralLine"))
        assertFalse(src.contains("open-meteo"))
        assertFalse(src.contains("LocalTemperature"))
        assertTrue(src.contains("home_empty"))
        assertTrue(src.contains("home_add"))
        assertTrue(src.contains("HorizontalPager"))
        assertTrue(blockUi.contains("santoral_novus.json"))
        assertTrue(src.contains("plan_ragazzini.json"))
        assertTrue(src.contains("HomePages.landingIndex"))
        assertTrue(src.contains("home_edit"))
        val defaults = HomePages.defaults()
        assertEquals(
            listOf(
                HomePages.TYPE_CLOCK,
                HomePages.TYPE_AGENDA,
                HomePages.TYPE_PERSONAL,
                HomePages.TYPE_DIET,
                HomePages.TYPE_WORK,
            ),
            defaults.map { it.type },
        )
        assertEquals(2, HomePages.landingIndex(HomePages.visible(defaults)))
        val clockFn = src.substringAfter("fun ClockHomePage").substringBefore("fun BlockPage")
        assertTrue(clockFn.contains("BlockPage"))
        assertFalse(clockFn.contains("nls_pause"))
        assertFalse(clockFn.contains("work_pause"))
        assertTrue(blockUi.contains("HomeClock"))
        assertTrue(blockUi.contains("santoral_1962.json"))
        assertTrue(blockUi.contains("santoral_novus.json"))
        assertTrue(blockUi.contains("SantoralLine"))
        assertTrue(blockUi.contains("showLabel = false"))
        assertEquals(2, Regex("showLabel = false").findAll(blockUi).count())
        assertTrue(blockUi.contains("PageBlocks.RELOJ"))
        assertTrue(blockUi.contains("PageBlocks.SILENCIO || type == PageBlocks.PAUSAR"))
        assertTrue(blockUi.contains("SilenceControl"))
        assertTrue(blockUi.contains("textAlign = TextAlign.Center"))
        assertTrue(src.contains("textAlign = TextAlign.Center"))
        assertFalse(blockUi.contains("open-meteo"))
        assertFalse(src.contains("open-meteo"))
        val personalFn = src.substringAfter("fun PersonalHomePage").substringBefore("fun WorkHomePage")
        assertTrue(personalFn.contains("section_personal"))
        assertTrue(personalFn.contains("SilenceControl"))
        assertFalse(personalFn.contains("nls_pause"))
        assertFalse(personalFn.contains("phone_pause"))
        val pill = File("src/main/java/com/foco/launcher/core/SilenceControl.kt").readText()
        assertTrue(pill.contains("silence_avisos"))
        assertTrue(pill.contains("silence_foco"))
        assertTrue(pill.contains("silence_a11y_off"))
        assertTrue(pill.contains("silence_a11y_on"))
        assertTrue(pill.contains("silence_activate"))
        assertFalse(pill.contains("silence_all"))
        assertFalse(pill.contains("silence_off"))
        assertTrue(pill.contains("Icons.Outlined.Notifications"))
        assertTrue(pill.contains("0.11f"))
        assertTrue(pill.contains("1.5.dp"))
        assertTrue(pill.contains("0.98f"))
        assertTrue(pill.contains("silence_a11y_name"))
        assertTrue(pill.contains("silence_a11y_hint_on"))
        assertTrue(pill.contains("silence_a11y_hint_off"))
        assertTrue(pill.contains("FocoInkElevated"))
        assertFalse(pill.contains("Icons.Filled.Notifications"))
        assertTrue(pill.contains("Silence.toggle"))
        assertTrue(src.contains("homeToken"))
        assertTrue(src.contains("scrollToPage"))
        assertTrue(src.contains("drawsPageTitle"))
        assertTrue(src.contains("showChromePill = visible.size < 2"))
        assertTrue(src.contains("beyondViewportPageCount = 1"))
        val activity = File("src/main/java/com/foco/launcher/core/LauncherActivity.kt").readText()
        assertTrue(activity.contains("homeToken++"))
        val silence = File("src/main/java/com/foco/launcher/notification/Silence.kt").readText()
        assertTrue(silence.contains("SilenceLevel.OFF -> SilenceLevel.FOCO"))
        assertTrue(silence.contains("SilenceLevel.FOCO -> SilenceLevel.OFF"))
        assertFalse(silence.contains("AVISOS"))
        assertFalse(silence.contains("TODO"))
        assertFalse(personalFn.contains("section_work"))
        assertFalse(personalFn.contains("work_pause"))
        val workFn = src.substringAfter("fun WorkHomePage").substringBefore("fun HomePagerCue")
        assertTrue(workFn.contains("section_work"))
        assertTrue(workFn.contains("work_pause"))
        assertFalse(workFn.contains("nls_pause"))
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
