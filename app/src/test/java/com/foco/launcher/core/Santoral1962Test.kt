package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class Santoral1962Test {
    private val catalog = Santoral1962.parse(File("src/main/assets/santoral_1962.json").readText())

    @Test
    fun assetIsThe1962MissalNotThe1969Calendar() {
        assertEquals("1962", catalog.calendar)
        assertEquals("1960", catalog.rubrics)
        assertEquals("vetus-ordo", catalog.rite)
        assertTrue(catalog.source.contains("1962 Missal"))
        assertTrue(catalog.source.contains("Not the 1969"))
        assertFalse(catalog.days.containsKey("09-25"))
        assertFalse(catalog.days.containsKey("05-08"))
        assertFalse(catalog.days.containsKey("01-02"))
        assertFalse(catalog.days.values.any { it.name.contains("Faustina") || it.name.contains("Bakhita") })
        assertFalse(catalog.days.values.any { it.name.contains("Madre de Dios") })
    }

    @Test
    fun septemberTwentyFifthIsAFeriaAndMichaelIsFirstClass() {
        val feria = Santoral1962.resolve(catalog, LocalDate.of(2026, 9, 25))
        assertEquals("Feria", feria.name)
        assertNull(feria.rank)
        assertTrue(feria.summary.contains("calendario tradicional"))

        val michael = Santoral1962.resolve(catalog, LocalDate.of(2026, 9, 29))
        assertTrue(michael.name.contains("Miguel"))
        assertTrue(michael.name.contains("Gabriel"))
        assertTrue(michael.name.contains("Rafael"))
        assertEquals("I", michael.rank)
        assertTrue(michael.summary.isNotBlank())
    }

    @Test
    fun fixedFeastsKeepThe1962Titles() {
        val octave = Santoral1962.resolve(catalog, LocalDate.of(2026, 1, 1))
        assertEquals("I", octave.rank)
        assertTrue(octave.name.contains("Octava"))
        assertFalse(octave.name.contains("Madre de Dios"))

        val christmas = Santoral1962.resolve(catalog, LocalDate.of(2026, 12, 25))
        assertEquals("Natividad del Señor", christmas.name)
        assertEquals("I", christmas.rank)

        val cross = Santoral1962.resolve(catalog, LocalDate.of(2026, 9, 14))
        assertEquals("II", cross.rank)
        assertTrue(cross.name.contains("Cruz"))

        val placid = catalog.days.getValue("10-05")
        assertTrue(placid.name.contains("Plácido"))
        assertTrue(catalog.days.getValue("02-08").name.contains("Juan de Mata"))
    }

    @Test
    fun moveableFeastsFollowEasterAndOutrankTheSanctorale() {
        assertEquals(LocalDate.of(2026, 4, 5), Santoral1962.easter(2026))
        assertEquals(LocalDate.of(2025, 4, 20), Santoral1962.easter(2025))
        val easter = Santoral1962.resolve(catalog, LocalDate.of(2026, 4, 5))
        assertEquals("Domingo de Resurrección", easter.name)
        val corpus = Santoral1962.resolve(catalog, LocalDate.of(2026, 6, 4))
        assertTrue(corpus.name.contains("Cuerpo"))
        assertEquals("I", corpus.rank)
        assertTrue(corpus.note.contains("Caracciolo"))
        val king = Santoral1962.resolve(catalog, LocalDate.of(2026, 10, 25))
        assertEquals("Cristo Rey", king.name)
    }
}
