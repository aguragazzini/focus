package com.foco.launcher.registry

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupMutationsTest {
    private val personal = setOf("phone", "settings", "messages", "camera")
    private val work = setOf("1:com.slack/.Main", "1:com.chrome/.Main")

    @Test
    fun createAssignsOrderAndKeepsTheMember() {
        val groups = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "g1", personal)
        assertEquals(1, groups.size)
        assertEquals(GroupSection.PERSONAL, groups[0].section)
        assertEquals("Casa", groups[0].name)
        assertEquals(0, groups[0].order)
        assertEquals(listOf("phone"), groups[0].members)
    }

    @Test
    fun createRejectsBlankNameAndForeignMember() {
        val start = emptyList<AppGroup>()
        assertEquals(start, GroupMutations.create(start, GroupSection.PERSONAL, "   ", "phone", "g1", personal))
        assertEquals(start, GroupMutations.create(start, GroupSection.PERSONAL, "Casa", "work.only", "g1", personal))
        assertNull(GroupMutations.normalizeName(" \n\t "))
    }

    @Test
    fun nameCollapsesSpaceAndTruncates() {
        assertEquals("Hola mundo", GroupMutations.normalizeName("  Hola   mundo  "))
        assertEquals(24, GroupMutations.normalizeName("a".repeat(40))?.length)
    }

    @Test
    fun secondGroupKeepsInsertionOrder() {
        val first = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "g1", personal)
        val second = GroupMutations.create(first, GroupSection.PERSONAL, "Fotos", "camera", "g2", personal)
        assertEquals(listOf(0, 1), second.map { it.order })
        assertEquals(listOf("g1", "g2"), second.map { it.id })
    }

    @Test
    fun createMovesMemberOutOfPreviousGroupInTheSameSectionOnly() {
        val workGroup = GroupMutations.create(
            emptyList(),
            GroupSection.WORK,
            "Oficina",
            "1:com.slack/.Main",
            "w1",
            work,
        )
        val personalGroup = GroupMutations.create(
            workGroup,
            GroupSection.PERSONAL,
            "Casa",
            "phone",
            "p1",
            personal,
        )
        val moved = GroupMutations.create(
            personalGroup,
            GroupSection.PERSONAL,
            "Dia",
            "phone",
            "p2",
            personal,
        )
        assertEquals(listOf("1:com.slack/.Main"), moved.first { it.id == "w1" }.members)
        assertFalse(moved.any { it.id == "p1" })
        assertEquals(listOf("phone"), moved.first { it.id == "p2" }.members)
    }

    @Test
    fun addRemoveRenameAndDelete() {
        val created = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "g1", personal)
        val added = GroupMutations.addMember(created, "g1", "messages", personal)
        assertEquals(listOf("phone", "messages"), added.single().members)

        val renamed = GroupMutations.rename(added, "g1", "  Hogar  ")
        assertEquals("Hogar", renamed.single().name)
        assertEquals(added, GroupMutations.rename(added, "g1", " "))

        val removed = GroupMutations.removeMember(renamed, "g1", "phone")
        assertEquals(listOf("messages"), removed.single().members)

        assertTrue(GroupMutations.removeMember(removed, "g1", "messages").isEmpty())
        val restored = GroupMutations.delete(renamed, "g1")
        assertTrue(restored.isEmpty())
    }

    @Test
    fun addMovesBetweenGroupsAndIgnoresUnknownGroup() {
        val first = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "g1", personal)
        val second = GroupMutations.create(first, GroupSection.PERSONAL, "Fotos", "camera", "g2", personal)
        val moved = GroupMutations.addMember(second, "g2", "phone", personal)
        assertEquals(listOf("camera", "phone"), moved.first { it.id == "g2" }.members)
        assertFalse(moved.any { it.id == "g1" })
        assertEquals(second, GroupMutations.addMember(second, "missing", "messages", personal))
        assertEquals(second, GroupMutations.addMember(second, "g2", "not-allowed", personal))
    }

    @Test
    fun duplicateIdDoesNotCreateAnotherGroup() {
        val created = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "g1", personal)
        val again = GroupMutations.create(created, GroupSection.PERSONAL, "Otra", "messages", "g1", personal)
        assertEquals(created, again)
    }

    @Test
    fun retainDropsWhitelistRemovalAndUninstallWithoutTouchingTheOtherSection() {
        var groups = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "p1", personal)
        groups = GroupMutations.addMember(groups, "p1", "messages", personal)
        groups = GroupMutations.create(groups, GroupSection.WORK, "Oficina", "1:com.slack/.Main", "w1", work)
        val afterWhitelist = GroupMutations.retain(groups, GroupSection.PERSONAL, setOf("messages", "camera"))
        assertEquals(listOf("messages"), afterWhitelist.first { it.id == "p1" }.members)
        assertEquals(listOf("1:com.slack/.Main"), afterWhitelist.first { it.id == "w1" }.members)

        val uninstalled = GroupMutations.retain(afterWhitelist, GroupSection.PERSONAL, setOf("camera"))
        assertFalse(uninstalled.any { it.section == GroupSection.PERSONAL })
        assertEquals(1, uninstalled.size)

        val profileGone = GroupMutations.retain(groups, GroupSection.WORK, emptySet())
        assertTrue(profileGone.none { it.section == GroupSection.WORK })
        assertEquals(listOf("phone", "messages"), profileGone.first { it.id == "p1" }.members)
    }

    @Test
    fun deleteRenormalizesOrderAndDoesNotImplyUninstall() {
        val entries = WhitelistMutations.addAll(emptyList(), listOf("phone" to false, "messages" to false))
        var groups = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "A", "phone", "g1", personal)
        groups = GroupMutations.create(groups, GroupSection.PERSONAL, "B", "messages", "g2", personal)
        val deleted = GroupMutations.delete(groups, "g1")
        assertEquals(listOf("g2"), deleted.map { it.id })
        assertEquals(listOf(0), deleted.map { it.order })
        val prefs = LauncherPrefs(entries = entries, groups = deleted)
        assertEquals(listOf("phone", "messages"), prefs.entries.map { it.packageName })
    }

    @Test
    fun withEntriesRemovesPersonalMembersAndKeepsWorkGroups() {
        var groups = GroupMutations.create(emptyList(), GroupSection.PERSONAL, "Casa", "phone", "p1", personal)
        groups = GroupMutations.addMember(groups, "p1", "settings", personal)
        groups = GroupMutations.create(groups, GroupSection.WORK, "Oficina", "1:com.chrome/.Main", "w1", work)
        val entries = WhitelistMutations.addAll(emptyList(), listOf("settings" to false, "camera" to false))
        val prefs = LauncherPrefs(entries = WhitelistMutations.add(entries, "phone"), groups = groups)
        val next = prefs.withEntries(WhitelistMutations.remove(prefs.entries, "phone"))
        assertEquals(listOf("settings", "camera"), next.entries.map { it.packageName })
        assertEquals(listOf("settings"), next.groups.first { it.id == "p1" }.members)
        assertTrue(next.groups.any { it.id == "w1" })

        val emptied = next.withEntries(WhitelistMutations.remove(next.entries, "settings"))
        assertFalse(emptied.groups.any { it.section == GroupSection.PERSONAL })
        assertTrue(emptied.groups.any { it.id == "w1" })
    }

    @Test
    fun v051JsonKeepsWhitelistAndSettingsWhenGroupsAreAbsent() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val raw = """
            {
              "setupDone": true,
              "biometricGlobalEnabled": false,
              "entries": [
                {"packageName":"com.android.settings","order":0,"bioEnabled":false,"allowNotif":true},
                {"packageName":"com.android.dialer","order":1,"bioEnabled":false,"allowNotif":false}
              ],
              "nlsFilterEnabled": true,
              "futureFlag": "ignore-me"
            }
        """.trimIndent()
        val prefs = json.decodeFromString<LauncherPrefs>(raw)
        assertTrue(prefs.setupDone)
        assertTrue(prefs.nlsFilterEnabled)
        assertFalse(prefs.biometricGlobalEnabled)
        assertEquals(
            listOf("com.android.settings" to true, "com.android.dialer" to false),
            prefs.entries.map { it.packageName to it.allowNotif },
        )
        assertTrue(prefs.groups.isEmpty())
        assertEquals(setOf("com.android.settings"), prefs.notificationAllowlist)

        val stored = json.decodeFromString<LauncherPrefs>(json.encodeToString(prefs))
        assertEquals(prefs, stored)

        val withGroup = prefs.copy(
            groups = GroupMutations.create(
                emptyList(),
                GroupSection.PERSONAL,
                "Casa",
                "com.android.settings",
                "g1",
                prefs.entries.map { it.packageName }.toSet(),
            ),
        )
        val roundTrip = json.decodeFromString<LauncherPrefs>(json.encodeToString(withGroup))
        assertEquals(withGroup, roundTrip)
        assertEquals("Casa", roundTrip.groups.single().name)
        assertTrue(roundTrip.setupDone)
        assertTrue(roundTrip.nlsFilterEnabled)
    }
}
