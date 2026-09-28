package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOrderTest {
    @Test
    fun spanishOrderIgnoresCaseAndKeepsAccentsAndEnie() {
        assertTrue(AppOrder.compare("árbol", "barco") < 0)
        assertTrue(AppOrder.compare("Árbol", "árbol") == 0)
        assertTrue(AppOrder.compare("arbol", "árbol") != 0)
        assertTrue(AppOrder.compare("nube", "ñandú") < 0)
        assertTrue(AppOrder.compare("ñandú", "oso") < 0)
        assertTrue(AppOrder.compare("Zeta", "alfa") > 0)
        assertTrue(AppOrder.compare("Beta", "alfa") > 0)
    }

    @Test
    fun tiesFallThroughToPackageThenClass() {
        data class Row(val label: String, val pkg: String, val cls: String)
        val sorted = AppOrder.byLabel(
            items = listOf(
                Row("alfa", "a.app", "A2"),
                Row("Beta", "b.app", "B"),
                Row("alfa", "a.app", "A1"),
                Row("Zeta", "z.app", "Z"),
                Row("Beta", "hidden.looking", "H"),
            ),
            label = { it.label },
            tieBreak = { "${it.pkg}\u0000${it.cls}" },
        )
        assertEquals(
            listOf("a.app/A1", "a.app/A2", "b.app/B", "hidden.looking/H", "z.app/Z"),
            sorted.map { "${it.pkg}/${it.cls}" },
        )
    }

    @Test
    fun whitelistStaysAlphabeticalUntilTheUserReorders() {
        val entries = listOf(
            WhitelistEntry("z.maps", 0),
            WhitelistEntry("a.phone", 1),
            WhitelistEntry("m.mail", 2),
        )
        val labels = mapOf("z.maps" to "Mapas", "a.phone" to "Teléfono", "m.mail" to "Correo")
        val alpha = WhitelistOrder.displayed(entries, customOrder = false) { labels[it] ?: it }
        assertEquals(listOf("m.mail", "z.maps", "a.phone"), alpha.map { it.packageName })
        val custom = WhitelistOrder.displayed(entries, customOrder = true) { labels[it] ?: it }
        assertEquals(listOf("z.maps", "a.phone", "m.mail"), custom.map { it.packageName })
    }

    @Test
    fun firstReorderFreezesTheAlphabeticalSequence() {
        val entries = listOf(
            WhitelistEntry("z.maps", 0),
            WhitelistEntry("a.phone", 1),
            WhitelistEntry("m.mail", 2),
        )
        val labels = mapOf("z.maps" to "Mapas", "a.phone" to "Teléfono", "m.mail" to "Correo")
        val shown = WhitelistOrder.displayed(entries, customOrder = false) { labels[it] ?: it }
        val moved = WhitelistMutations.reorderDisplayed(shown, fromIndex = 2, toIndex = 0)
        assertEquals(listOf("a.phone", "m.mail", "z.maps"), moved.map { it.packageName })
        assertEquals(listOf(0, 1, 2), moved.map { it.order })
        val again = WhitelistOrder.displayed(moved, customOrder = true) { labels[it] ?: it }
        assertEquals(moved.map { it.packageName }, again.map { it.packageName })
    }
}
