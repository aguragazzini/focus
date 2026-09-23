package com.foco.launcher.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkCatalogRulesTest {
    @Test
    fun presentAllKeepsEveryLaunchable() {
        val input = listOf(
            WorkActivityId("b.app", "B", "Beta"),
            WorkActivityId("a.app", "A2", "alfa"),
            WorkActivityId("a.app", "A1", "alfa"),
            WorkActivityId("z.app", "Z", "Zeta"),
            WorkActivityId("hidden.looking", "H", "Beta"),
        )
        val shown = WorkCatalogRules.presentAll(
            activities = input,
            label = { it.label },
            packageName = { it.packageName },
            className = { it.className },
        )
        assertEquals(input.size, shown.size)
        assertEquals(
            input.map { "${it.packageName}/${it.className}" }.toSet(),
            shown.map { "${it.packageName}/${it.className}" }.toSet(),
        )
        assertEquals(
            listOf("a.app/A1", "a.app/A2", "b.app/B", "hidden.looking/H", "z.app/Z"),
            shown.map { "${it.packageName}/${it.className}" },
        )
    }

    @Test
    fun emptyListStaysEmptyAndIsNotReplaced() {
        assertTrue(
            WorkCatalogRules.presentAll(
                activities = emptyList<WorkActivityId>(),
                label = { it.label },
                packageName = { it.packageName },
                className = { it.className },
            ).isEmpty(),
        )
    }

    @Test
    fun sectionFollowsProfilePresenceOnly() {
        assertFalse(WorkCatalogRules.showSection(hasWorkProfile = false))
        assertTrue(WorkCatalogRules.showSection(hasWorkProfile = true))
    }
}
