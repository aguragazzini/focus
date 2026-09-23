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

    @Test
    fun sectionKindCoversS0ThroughS4WithoutInventingQuiet() {
        assertEquals(
            WorkSectionKind.Hidden,
            WorkCatalogRules.sectionKind(false, loadFailed = false, quietEnabled = null, activityCount = 0),
        )
        assertEquals(
            WorkSectionKind.Active,
            WorkCatalogRules.sectionKind(true, loadFailed = false, quietEnabled = null, activityCount = 3),
        )
        assertEquals(
            WorkSectionKind.Active,
            WorkCatalogRules.sectionKind(true, loadFailed = false, quietEnabled = false, activityCount = 3),
        )
        assertEquals(
            WorkSectionKind.Empty,
            WorkCatalogRules.sectionKind(true, loadFailed = false, quietEnabled = false, activityCount = 0),
        )
        assertEquals(
            WorkSectionKind.Quiet,
            WorkCatalogRules.sectionKind(true, loadFailed = false, quietEnabled = true, activityCount = 4),
        )
        assertEquals(
            WorkSectionKind.Quiet,
            WorkCatalogRules.sectionKind(true, loadFailed = false, quietEnabled = true, activityCount = 0),
        )
        assertEquals(
            WorkSectionKind.Error,
            WorkCatalogRules.sectionKind(true, loadFailed = true, quietEnabled = null, activityCount = 0),
        )
        assertEquals(
            WorkSectionKind.Error,
            WorkCatalogRules.sectionKind(false, loadFailed = true, quietEnabled = null, activityCount = 0),
        )
    }

    @Test
    fun quietKeepsTheGridAndDoesNotUseTheEmptyCopy() {
        val kind = WorkCatalogRules.sectionKind(true, false, quietEnabled = true, activityCount = 5)
        assertEquals(WorkSectionKind.Quiet, kind)
        assertTrue(WorkCatalogRules.showWorkGrid(kind, visibleCount = 5))
        assertTrue(WorkCatalogRules.showQuietCopy(kind))
        assertFalse(WorkCatalogRules.showWorkEmptyCopy(kind))
        assertEquals(5, WorkCatalogRules.headerCount(5))
    }

    @Test
    fun unknownQuietStaysActive() {
        val kind = WorkCatalogRules.sectionKind(true, false, quietEnabled = null, activityCount = 2)
        assertEquals(WorkSectionKind.Active, kind)
        assertFalse(WorkCatalogRules.showQuietCopy(kind))
    }

    @Test
    fun combineQuietRequiresARealTrue() {
        assertEquals(true, WorkCatalogRules.combineQuiet(listOf(false, true)))
        assertEquals(false, WorkCatalogRules.combineQuiet(listOf(false, false)))
        assertEquals(null, WorkCatalogRules.combineQuiet(listOf(null, null)))
        assertEquals(null, WorkCatalogRules.combineQuiet(listOf(false, null)))
        assertEquals(null, WorkCatalogRules.combineQuiet(emptyList()))
    }

    @Test
    fun emptyQueryKeepsEveryLaunchableForA1() {
        val catalog = listOf(
            WorkActivityId("z.app", "Z", "Zeta"),
            WorkActivityId("a.app", "A", "Alfa"),
        )
        val ordered = WorkCatalogRules.presentAll(
            activities = catalog,
            label = { it.label },
            packageName = { it.packageName },
            className = { it.className },
        )
        val visible = WorkCatalogRules.filterVisible(
            items = ordered,
            query = "   ",
            label = { it.label },
            packageName = { it.packageName },
        )
        assertTrue(visible === ordered)
        assertEquals(ordered.size, visible.size)
        assertEquals(
            ordered.size,
            WorkCatalogRules.acceptanceCount(ordered.size, visible.size, "   "),
        )
    }

    @Test
    fun searchFiltersTheGridOnly() {
        val catalog = listOf(
            WorkActivityId("mail.work", "M", "Correo"),
            WorkActivityId("chat.work", "C", "Chat"),
            WorkActivityId("maps.work", "P", "Mapas"),
        )
        val visible = WorkCatalogRules.filterVisible(
            items = catalog,
            query = "cor",
            label = { it.label },
            packageName = { it.packageName },
        )
        assertEquals(1, visible.size)
        assertEquals("mail.work", visible.single().packageName)
        assertEquals(3, catalog.size)
        assertEquals(catalog.size, WorkCatalogRules.headerCount(catalog.size))
        assertEquals(1, WorkCatalogRules.acceptanceCount(catalog.size, visible.size, "cor"))
        assertEquals(catalog.size, WorkCatalogRules.acceptanceCount(catalog.size, visible.size, ""))
    }

    @Test
    fun searchFieldAppearsAt24() {
        assertFalse(WorkCatalogRules.showSearch(WorkSectionKind.Active, 23))
        assertTrue(WorkCatalogRules.showSearch(WorkSectionKind.Active, 24))
        assertTrue(WorkCatalogRules.showSearch(WorkSectionKind.Quiet, 30))
        assertFalse(WorkCatalogRules.showSearch(WorkSectionKind.Empty, 24))
        assertFalse(WorkCatalogRules.showSearch(WorkSectionKind.Hidden, 40))
    }

    @Test
    fun searchMissCopyIsNotTheEmptyProfileCopy() {
        val kind = WorkSectionKind.Active
        assertFalse(WorkCatalogRules.showWorkEmptyCopy(kind))
        assertFalse(WorkCatalogRules.showWorkGrid(kind, visibleCount = 0))
        assertTrue(WorkCatalogRules.showWorkEmptyCopy(WorkSectionKind.Empty))
    }

    @Test
    fun refreshNoticesListAndQuietChanges() {
        val base = WorkSnapshot(profile = true, failed = false, quiet = false, keys = listOf("a"))
        assertFalse(WorkCatalogRules.refreshChanged(base, base.copy()))
        assertTrue(WorkCatalogRules.refreshChanged(base, base.copy(keys = listOf("a", "b"))))
        assertTrue(WorkCatalogRules.refreshChanged(base, base.copy(quiet = true)))
        assertTrue(WorkCatalogRules.refreshChanged(base, base.copy(failed = true, keys = emptyList())))
    }

    @Test
    fun settingsRowReflectsS0S1S3AndFailure() {
        assertEquals(
            WorkSettingsStatus.Absent,
            WorkCatalogRules.settingsStatus(true, hasProfile = false, quietEnabled = null, loadFailed = false, activityCount = 0),
        )
        assertEquals(
            WorkSettingsStatus.Visible,
            WorkCatalogRules.settingsStatus(true, hasProfile = true, quietEnabled = false, loadFailed = false, activityCount = 8),
        )
        assertEquals(
            WorkSettingsStatus.Visible,
            WorkCatalogRules.settingsStatus(true, hasProfile = true, quietEnabled = null, loadFailed = false, activityCount = 0),
        )
        assertEquals(
            WorkSettingsStatus.Quiet,
            WorkCatalogRules.settingsStatus(true, hasProfile = true, quietEnabled = true, loadFailed = false, activityCount = 8),
        )
        assertEquals(
            WorkSettingsStatus.Failed,
            WorkCatalogRules.settingsStatus(true, hasProfile = false, quietEnabled = null, loadFailed = true, activityCount = 0),
        )
        assertEquals(
            WorkSettingsStatus.Unknown,
            WorkCatalogRules.settingsStatus(false, hasProfile = false, quietEnabled = null, loadFailed = false, activityCount = 0),
        )
    }
}
