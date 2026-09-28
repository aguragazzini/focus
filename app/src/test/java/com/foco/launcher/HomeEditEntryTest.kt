package com.foco.launcher

import com.foco.launcher.core.HomeReturn
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HomeEditEntryTest {
    @Test
    fun plainReturnResetsHome() {
        assertTrue(HomeReturn.resets(editRequested = false, openPage = null))
        assertTrue(HomeReturn.resets(editRequested = false, openPage = "  "))
    }

    @Test
    fun editOrOpenPageDoesNotResetHome() {
        assertFalse(HomeReturn.resets(editRequested = true, openPage = null))
        assertFalse(HomeReturn.resets(editRequested = true, openPage = "USO"))
        assertFalse(HomeReturn.resets(editRequested = false, openPage = "USO"))
    }

    @Test
    fun pagerCueClickOpensEdit() {
        val src = File("src/main/java/com/foco/launcher/core/HomeScreen.kt").readText()
        val cue = src.substringAfter("fun HomePagerCue").substringBefore("fun PagePause")
        assertTrue(cue.contains("onClick = onEdit"))
        assertFalse(cue.contains("onClick = {}"))
        assertTrue(cue.contains("onLongClick = onEdit"))
        assertTrue(src.contains("if (homeToken > 0 && !requestEdit) editing = false"))
    }

    @Test
    fun homeTokenWaitsUntilTheEditIntentIsCurrent() {
        val activity = File("src/main/java/com/foco/launcher/core/LauncherActivity.kt").readText()
        val restart = activity.substringAfter("override fun onRestart()").substringBefore("override fun onResume()")
        val resume = activity.substringAfter("override fun onResume()").substringBefore("companion object")
        assertFalse(restart.contains("homeToken++"))
        assertTrue(resume.contains("homeToken++"))
        assertTrue(resume.contains("HomeReturn.resets"))
    }
}
