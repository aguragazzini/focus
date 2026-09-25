package com.foco.launcher.registry

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePagesTest {
    @Test
    fun defaultsAreTheFourPagesLandingOnPersonal() {
        val pages = HomePages.defaults()
        assertEquals(
            listOf(
                HomePages.TYPE_CLOCK,
                HomePages.TYPE_PERSONAL,
                HomePages.TYPE_DIET,
                HomePages.TYPE_WORK,
            ),
            pages.map { it.type },
        )
        assertTrue(pages.none { it.hidden })
        assertEquals(1, HomePages.landingIndex(HomePages.visible(pages)))
    }

    @Test
    fun emptyOrCorruptLayoutRestoresTheDefaultFour() {
        assertEquals(HomePages.defaults(), HomePages.resolve(emptyList()))
        val garbage = listOf(
            HomePageSpec(id = "", type = HomePages.TYPE_CLOCK),
            HomePageSpec(id = "x", type = "WEATHER"),
            HomePageSpec(id = "x", type = "DOCK"),
        )
        assertEquals(HomePages.defaults(), HomePages.resolve(garbage))
        val allHidden = HomePages.defaults().map { it.copy(hidden = true) }
        assertEquals(HomePages.defaults(), HomePages.resolve(allHidden))
    }

    @Test
    fun partialJunkKeepsTheValidRows() {
        val raw = listOf(
            HomePageSpec(id = "a", type = "clock", label = "  Casa   grande  "),
            HomePageSpec(id = "a", type = HomePages.TYPE_WORK),
            HomePageSpec(id = " ", type = HomePages.TYPE_DIET),
            HomePageSpec(id = "b", type = HomePages.TYPE_APPS, hidden = true),
        )
        val resolved = HomePages.resolve(raw)
        assertEquals(listOf("a", "b"), resolved.map { it.id })
        assertEquals(HomePages.TYPE_CLOCK, resolved[0].type)
        assertEquals("Casa grande", resolved[0].label)
        assertTrue(resolved[1].hidden)
        assertEquals(listOf("a"), HomePages.visible(resolved).map { it.id })
    }

    @Test
    fun hideAndDeleteNeverRemoveTheLastVisiblePage() {
        val pages = HomePages.defaults()
        val only = listOf(pages.first())
        assertEquals(only, HomePages.setHidden(only, only.first().id, true))
        assertEquals(only, HomePages.delete(only, only.first().id))
        assertFalse(HomePages.canDelete(only, only.first().id))

        val hiddenTail = pages.mapIndexed { index, page ->
            if (index == 0) page else page.copy(hidden = true)
        }
        assertEquals(hiddenTail, HomePages.setHidden(hiddenTail, hiddenTail.first().id, true))
        assertEquals(hiddenTail, HomePages.delete(hiddenTail, hiddenTail.first().id))
        val withoutHidden = HomePages.delete(hiddenTail, hiddenTail[1].id)
        assertEquals(3, withoutHidden.size)
        assertTrue(withoutHidden.none { it.id == hiddenTail[1].id })
    }

    @Test
    fun createRenameMoveAndRepeatTypes() {
        val start = HomePages.defaults()
        val created = HomePages.create(start, "apps", "  Vacío  ", "page-apps")
        assertEquals(5, created.size)
        assertEquals(HomePages.TYPE_APPS, created.last().type)
        assertEquals("Vacío", created.last().label)
        assertEquals(start, HomePages.create(start, "NOPE", "X", "n"))
        assertEquals(start, HomePages.create(start, HomePages.TYPE_CLOCK, "X", "page-clock"))
        assertEquals(start, HomePages.create(start, HomePages.TYPE_CLOCK, "X", " "))
        val full = List(HomePages.MAX) { index ->
            HomePageSpec(id = "p$index", type = HomePages.TYPE_PERSONAL)
        }
        assertEquals(full, HomePages.create(full, HomePages.TYPE_CLOCK, "Otro", "extra"))

        val renamed = HomePages.rename(created, "page-apps", "   ")
        assertEquals("", renamed.last().label)
        val capped = "n".repeat(40)
        assertEquals(GroupMutations.NAME_MAX, HomePages.rename(created, "page-apps", capped).last().label.length)
        assertEquals(created, HomePages.rename(created, "missing", "Hola"))

        val moved = HomePages.move(start, "page-work", -1)
        assertEquals(HomePages.TYPE_WORK, moved[2].type)
        assertEquals(HomePages.TYPE_DIET, moved[3].type)
        assertEquals(start, HomePages.move(start, "page-clock", -1))
        assertEquals(start, HomePages.move(start, "missing", 1))

        val secondClock = HomePages.create(start, HomePages.TYPE_CLOCK, "", "page-clock-2")
        assertEquals(2, secondClock.count { it.type == HomePages.TYPE_CLOCK })
        assertEquals(1, HomePages.landingIndex(HomePages.visible(secondClock)))
    }

    @Test
    fun oldPrefsWithoutPagesDecodeToTheDefaultLayout() {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val decoded = json.decodeFromString<LauncherPrefs>("""{"setupDone":true,"entries":[]}""")
        assertTrue(decoded.pages.isEmpty())
        assertEquals(
            listOf(
                HomePages.TYPE_CLOCK,
                HomePages.TYPE_PERSONAL,
                HomePages.TYPE_DIET,
                HomePages.TYPE_WORK,
            ),
            HomePages.resolve(decoded.pages).map { it.type },
        )
        val stored = json.decodeFromString<LauncherPrefs>(
            json.encodeToString(
                LauncherPrefs(
                    pages = listOf(
                        HomePageSpec(id = "only", type = HomePages.TYPE_WORK, label = "Oficina"),
                        HomePageSpec(id = "bad", type = "DOCK"),
                    ),
                ),
            ),
        )
        val resolved = HomePages.resolve(stored.pages)
        assertEquals(listOf("only"), resolved.map { it.id })
        assertEquals("Oficina", resolved.single().label)
        assertEquals(0, HomePages.landingIndex(HomePages.visible(resolved)))
    }
}
