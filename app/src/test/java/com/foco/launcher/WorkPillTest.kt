package com.foco.launcher

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WorkPillTest {
    @Test
    fun trabajoPauseUsesThePersonalPillChrome() {
        val home = File("src/main/java/com/foco/launcher/core/HomeScreen.kt").readText()
        val personal = home.substringAfter("fun PersonalHomePage").substringBefore("fun WorkHomePage")
        val work = home.substringAfter("fun WorkHomePage").substringBefore("fun pageLabel")
        assertTrue(personal.contains("SilenceControl("))
        assertFalse(personal.contains("FocoStatePill("))
        assertFalse(personal.contains("work_pill_"))
        assertTrue(work.contains("FocoStatePill("))
        assertTrue(work.contains("work_pill_off"))
        assertTrue(work.contains("work_pill_on"))
        assertTrue(work.contains("onWorkPaused(!state.workSectionPaused)"))
        assertFalse(work.contains("PagePause("))
        assertTrue(work.contains("showTitle = false"))
        assertFalse(work.contains("requestQuietModeEnabled"))

        val pill = File("src/main/java/com/foco/launcher/core/SilenceControl.kt").readText()
        val silence = pill.substringAfter("fun SilenceControl").substringBefore("fun FocoStatePill")
        val chrome = pill.substringAfter("fun FocoStatePill")
        assertTrue(silence.contains("FocoStatePill("))
        assertTrue(silence.contains("silence_avisos"))
        assertTrue(silence.contains("silence_foco"))
        assertTrue(silence.contains("Icons.Outlined.Notifications"))
        assertTrue(silence.contains("Silence.toggle"))
        assertTrue(chrome.contains("0.11f"))
        assertTrue(chrome.contains("1.5.dp"))
        assertTrue(chrome.contains("0.98f"))
        assertTrue(chrome.contains("FocoInkElevated"))
        assertTrue(chrome.contains("FocoSpace.touch"))
        assertTrue(chrome.contains("16.sp"))
        assertFalse(chrome.contains("silence_avisos"))
        assertFalse(chrome.contains("work_pill_"))

        val xml = File("src/main/res/values/strings.xml").readText()
        assertTrue(xml.contains("<string name=\"work_pill_off\">Trabajo</string>"))
        assertTrue(xml.contains("<string name=\"work_pill_on\">Pausado</string>"))
        assertTrue(xml.contains("<string name=\"silence_avisos\">Avisos</string>"))
        assertTrue(xml.contains("<string name=\"silence_foco\">Foco</string>"))
        assertTrue(xml.contains("<string name=\"work_pause\">Pausar Trabajo</string>"))
    }
}
