package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class SantoralNovusTest {
    private val catalog = SantoralNovus.parse(File("src/main/assets/santoral_novus.json").readText())

    @Test
    fun assetIsThe1969CalendarNotThe1962Missal() {
        assertEquals("novus", catalog.calendar)
        assertEquals("1969", catalog.rubrics)
        assertEquals("novus-ordo", catalog.rite)
        assertTrue(catalog.source.contains("1969"))
        assertTrue(catalog.source.contains("Not the 1962"))
        assertEquals("Santa María, Madre de Dios", catalog.days.getValue("01-01").name)
        assertFalse(catalog.days.values.any { it.name.contains("Octava de Navidad") })
    }

    @Test
    fun januaryFirstAndChristTheKingDifferFrom1962() {
        val vetus = Santoral1962.parse(File("src/main/assets/santoral_1962.json").readText())
        val mother = SantoralNovus.resolve(catalog, LocalDate.of(2026, 1, 1))
        assertTrue(mother.name.contains("Madre de Dios"))
        assertEquals("S", mother.rank)
        assertFalse(mother.name == Santoral1962.resolve(vetus, LocalDate.of(2026, 1, 1)).name)

        val king = SantoralNovus.resolve(catalog, LocalDate.of(2026, 11, 22))
        assertTrue(king.name.contains("Rey"))
        assertEquals("S", king.rank)
        val october = SantoralNovus.resolve(catalog, LocalDate.of(2026, 10, 25))
        assertFalse(october.name.contains("Rey"))
        assertEquals("Cristo Rey", Santoral1962.resolve(vetus, LocalDate.of(2026, 10, 25)).name)
    }

    @Test
    fun moveableFeastsUseSundayTransfersAndOrdinaryTime() {
        val easter = SantoralNovus.resolve(catalog, LocalDate.of(2026, 4, 5))
        assertEquals("Domingo de Pascua", easter.name)
        val ascension = SantoralNovus.resolve(catalog, LocalDate.of(2026, 5, 17))
        assertTrue(ascension.name.contains("Ascensión"))
        assertFalse(SantoralNovus.resolve(catalog, LocalDate.of(2026, 5, 14)).name.contains("Ascensión"))
        val corpus = SantoralNovus.resolve(catalog, LocalDate.of(2026, 6, 7))
        assertTrue(corpus.name.contains("Cuerpo"))
        assertFalse(SantoralNovus.resolve(catalog, LocalDate.of(2026, 6, 4)).name.contains("Cuerpo"))

        val friday = SantoralNovus.resolve(catalog, LocalDate.of(2026, 9, 25))
        assertTrue(friday.name.contains("Viernes"))
        assertTrue(friday.name.contains("25"))
        assertNull(friday.rank)
        val archangels = SantoralNovus.resolve(catalog, LocalDate.of(2026, 9, 29))
        assertTrue(archangels.name.contains("Miguel"))
        assertEquals("F", archangels.rank)
    }

    @Test
    fun impededSolemnitiesMoveAndABrokenAssetFailsSoft() {
        val joseph = SantoralNovus.resolve(catalog, LocalDate.of(2023, 3, 20))
        assertTrue(joseph.name.contains("José"))
        assertFalse(SantoralNovus.resolve(catalog, LocalDate.of(2023, 3, 19)).name.contains("José"))

        val annunciation = SantoralNovus.resolve(catalog, LocalDate.of(2024, 4, 8))
        assertTrue(annunciation.name.contains("Anunciación"))
        assertFalse(SantoralNovus.resolve(catalog, LocalDate.of(2024, 3, 25)).name.contains("Anunciación"))

        assertTrue(SantoralNovus.resolve(catalog, LocalDate.of(2025, 11, 3)).name.contains("difuntos"))
        assertFalse(SantoralNovus.resolve(catalog, LocalDate.of(2025, 11, 2)).name.contains("difuntos"))

        assertNull(SantoralNovus.read("{"))
        val empty = SantoralCatalog(
            calendar = "novus",
            rubrics = "1969",
            rite = "novus-ordo",
            source = "test",
            days = emptyMap(),
        )
        assertEquals("Domingo de Pascua", SantoralNovus.resolve(empty, LocalDate.of(2026, 4, 5)).name)
    }
}
