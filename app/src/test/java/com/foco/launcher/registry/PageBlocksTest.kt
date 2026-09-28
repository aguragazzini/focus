package com.foco.launcher.registry

import com.foco.launcher.core.PageBlockLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PageBlocksTest {
    @Test
    fun catalogHasTheShortModulesAndStableIds() {
        assertTrue(PageBlocks.ALL.size >= 28)
        assertEquals(PageBlocks.ALL.size, PageBlocks.ALL.toSet().size)
        assertTrue(PageBlocks.RELOJ in PageBlocks.ALL)
        assertTrue(PageBlocks.VACIO in PageBlocks.ALL)
        assertTrue(PageBlocks.allowsDuplicate(PageBlocks.NOTA))
        assertTrue(PageBlocks.allowsDuplicate(PageBlocks.CONTADOR))
        assertTrue(PageBlocks.allowsDuplicate(PageBlocks.FAVORITOS))
        assertTrue(PageBlocks.allowsDuplicate(PageBlocks.VACIO))
        assertFalse(PageBlocks.allowsDuplicate(PageBlocks.VETUS))
    }

    @Test
    fun defaultsMatchTheCurrentPagesAndSantoralIsRemovable() {
        val clock = HomePageSpec(id = "page-clock", type = HomePages.TYPE_CLOCK)
        assertEquals(
            listOf(
                PageBlocks.RELOJ,
                PageBlocks.FECHA,
                PageBlocks.BATERIA,
                PageBlocks.ALARMA,
                PageBlocks.VETUS,
                PageBlocks.NOVUS,
            ),
            PageBlocks.effective(clock).map { it.type },
        )
        assertEquals(listOf(PageBlocks.AGENDA), PageBlocks.effective(HomePageSpec("page-agenda", HomePages.TYPE_AGENDA)).map { it.type })
        assertEquals(listOf(PageBlocks.COMIDA), PageBlocks.effective(HomePageSpec("page-diet", HomePages.TYPE_DIET)).map { it.type })
        assertTrue(PageBlocks.effective(HomePageSpec("page-personal", HomePages.TYPE_PERSONAL)).isEmpty())
        assertTrue(PageBlocks.effective(HomePageSpec("page-work", HomePages.TYPE_WORK)).isEmpty())

        val withoutSaints = PageBlocks.effective(clock).filter { it.type != PageBlocks.VETUS && it.type != PageBlocks.NOVUS }
        val edited = clock.copy(blocks = withoutSaints, blocksSet = true)
        assertEquals(listOf(PageBlocks.RELOJ, PageBlocks.FECHA, PageBlocks.BATERIA, PageBlocks.ALARMA), PageBlocks.effective(edited).map { it.type })
    }

    @Test
    fun addRefusesASecondClockAndAllowsASecondNote() {
        val start = PageBlocks.effective(HomePageSpec("page-clock", HomePages.TYPE_CLOCK))
        assertEquals(start, PageBlocks.add(start, PageBlocks.RELOJ, "again"))
        val withNote = PageBlocks.add(start, PageBlocks.NOTA, "n1")
        val two = PageBlocks.add(withNote, PageBlocks.NOTA, "n2")
        assertEquals(2, two.count { it.type == PageBlocks.NOTA })
        val moved = PageBlocks.move(two, "n2", -1)
        assertEquals("n2", moved[moved.lastIndex - 1].id)
        assertEquals(two.size - 1, PageBlocks.remove(two, "n1").size)
    }

    @Test
    fun oldPageJsonWithoutBlocksDecodes() {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        val page = json.decodeFromString<HomePageSpec>("""{"id":"page-clock","type":"CLOCK"}""")
        assertFalse(page.blocksSet)
        assertTrue(page.blocks.isEmpty())
        assertEquals(PageBlocks.RELOJ, PageBlocks.effective(page).first().type)
    }

    @Test
    fun moonWeekMealAndWeatherParseStayLocal() {
        assertEquals(PageBlockLogic.MOON_NEW, PageBlockLogic.moonPhase(947182440000L))
        val day = LocalDate.of(2026, 9, 28)
        assertTrue(PageBlockLogic.weekNumber(day) in 1..53)
        assertEquals("lun", PageBlockLogic.weekdayShort(LocalDate.of(2026, 9, 28)))
        assertEquals("desayuno", PageBlockLogic.mealSlot(8, weekend = false))
        assertEquals("almuerzo", PageBlockLogic.mealSlot(13, weekend = false))
        assertEquals("cena", PageBlockLogic.mealSlot(20, weekend = false))
        assertEquals("finde", PageBlockLogic.mealSlot(12, weekend = true))
        assertEquals(22, PageBlockLogic.parseOpenMeteoCelsius("""{"current":{"temperature_2m":21.6}}"""))
        assertNull(PageBlockLogic.parseOpenMeteoCelsius("{}"))
        assertEquals("Una", PageBlockLogic.pickPhrase(listOf("Una", "Dos"), LocalDate.of(2026, 1, 2)))
        assertEquals("1,0 GB libres", PageBlockLogic.storageLabel(1024L * 1024L * 1024L))
    }
}
