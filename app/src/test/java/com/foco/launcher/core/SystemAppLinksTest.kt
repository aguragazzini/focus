package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemAppLinksTest {
    @Test
    fun clockPrefersShowAlarmsAndNeverWeather() {
        val clock = SystemAppLinks.clockCandidates()
        assertEquals(SystemAppLinks.ACTION_SHOW_ALARMS, clock.first().action)
        assertTrue(clock.first().launcherPackage == null)
        val blob = (clock + SystemAppLinks.calendarCandidates()).joinToString { candidate ->
            candidate.action + " " + candidate.categories.joinToString() + " " + candidate.launcherPackage.orEmpty()
        }
        assertFalse(blob.contains("http"))
        assertFalse(blob.contains("weather"))
        assertFalse(blob.contains("climate"))
    }

    @Test
    fun calendarPrefersTheAppCalendarCategory() {
        val calendar = SystemAppLinks.calendarCandidates()
        assertTrue(calendar.first().categories.contains(SystemAppLinks.CATEGORY_APP_CALENDAR))
        assertTrue(calendar.first().launcherPackage == null)
    }
}
