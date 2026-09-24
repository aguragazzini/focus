package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupLayoutTest {
    @Test
    fun groupsComeFirstThenLooseAppsInExistingOrder() {
        val groups = listOf(
            AppGroup("g2", GroupSection.PERSONAL, "Fotos", order = 1, members = listOf("camera")),
            AppGroup("g1", GroupSection.PERSONAL, "Casa", order = 0, members = listOf("messages", "phone")),
            AppGroup("w1", GroupSection.WORK, "Oficina", order = 0, members = listOf("slack")),
        )
        val arranged = GroupLayout.arrange(
            GroupSection.PERSONAL,
            groups,
            orderedIds = listOf("phone", "settings", "messages", "camera"),
        )
        assertEquals(listOf("g1", "g2"), arranged.groups.map { it.id })
        assertEquals(listOf("messages", "phone"), arranged.groups[0].members)
        assertEquals(listOf("settings"), arranged.looseIds)
        assertTrue(arranged.groups.none { it.section == GroupSection.WORK })
    }

    @Test
    fun ghostMembersAndEmptyGroupsStayOffTheGrid() {
        val groups = listOf(
            AppGroup("gone", GroupSection.PERSONAL, "Viejo", order = 0, members = listOf("uninstalled")),
            AppGroup("mixed", GroupSection.PERSONAL, "Casa", order = 1, members = listOf("uninstalled", "phone")),
        )
        val arranged = GroupLayout.arrange(
            GroupSection.PERSONAL,
            groups,
            orderedIds = listOf("phone", "settings"),
        )
        assertEquals(listOf("mixed"), arranged.groups.map { it.id })
        assertEquals(listOf("phone"), arranged.groups.single().members)
        assertEquals(listOf("settings"), arranged.looseIds)
    }

    @Test
    fun workOrderFollowsTheCatalogExceptGroupedApps() {
        val groups = listOf(
            AppGroup("w1", GroupSection.WORK, "Oficina", order = 0, members = listOf("slack", "missing")),
        )
        val arranged = GroupLayout.arrange(
            GroupSection.WORK,
            groups,
            orderedIds = listOf("chrome", "slack", "docs"),
        )
        assertEquals(listOf("slack"), arranged.groups.single().members)
        assertEquals(listOf("chrome", "docs"), arranged.looseIds)
    }
}
