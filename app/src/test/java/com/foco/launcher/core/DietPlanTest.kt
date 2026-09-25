package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class DietPlanTest {
    private val catalog = DietPlan.parse(File("src/main/assets/plan_ragazzini.json").readText())

    @Test
    fun mondayIsChickenWithAgusBreakfast() {
        val day = DietPlan.resolve(catalog, LocalDate.of(2026, 9, 28))
        assertEquals("Día del pollo", day?.theme)
        assertEquals("Hoy · Día del pollo", day?.let(DietPlan::homeTitle))
        assertTrue(day!!.lunch.contains("Pollo"))
        assertTrue(day.dinner.contains("Tortilla"))
        assertTrue(day.breakfast.contains("Huevos"))
        assertTrue(day.lunchTip.contains("oliva"))
        assertEquals(3, day.alternateTitles.size)
        assertFalse(day.weekend)
    }

    @Test
    fun tuesdayBreakfastIsVegetableOmelette() {
        val day = DietPlan.resolve(catalog, LocalDate.of(2026, 9, 29))
        assertEquals("Día de la carne vacuna", day?.theme)
        assertTrue(day!!.breakfast.contains("Tortilla"))
        assertTrue(day.dinnerTip.contains("palta"))
    }

    @Test
    fun fridayIsFishAndWeekendIsOneSoftLine() {
        val friday = DietPlan.resolve(catalog, LocalDate.of(2026, 9, 25))
        assertEquals("Día del pescado", friday?.theme)
        assertTrue(friday!!.breakfast.contains("Huevos"))
        val saturday = DietPlan.resolve(catalog, LocalDate.of(2026, 9, 26))
        val sunday = DietPlan.resolve(catalog, LocalDate.of(2026, 9, 27))
        assertTrue(saturday!!.weekend)
        assertTrue(sunday!!.weekend)
        assertEquals(
            "Fin de semana · misma base (proteína + verdura)",
            DietPlan.homeTitle(saturday),
        )
        assertTrue(saturday.lunch.isEmpty())
        assertTrue(saturday.rules.any { it.contains("picoteo", ignoreCase = true) })
        assertTrue(saturday.rules.any { it.contains("caminata", ignoreCase = true) })
        assertTrue(saturday.alternateTitles.isEmpty())
    }

    @Test
    fun missingWeekdayFailsSoft() {
        val empty = DietPlan.parse("""{"weekend":{"title":"","rules":[]},"days":[]}""")
        assertNull(DietPlan.resolve(empty, LocalDate.of(2026, 9, 28)))
        assertNull(DietPlan.resolve(empty, LocalDate.of(2026, 9, 26)))
        val extra = DietPlan.parse(
            """{"note":"ignored","weekend":{"title":"Fin","rules":["Tres comidas."]},"days":[]}""",
        )
        val weekend = DietPlan.resolve(extra, LocalDate.of(2026, 9, 26))
        assertEquals("Fin", weekend?.theme)
        assertEquals(listOf("Tres comidas."), weekend?.rules)
    }
}
