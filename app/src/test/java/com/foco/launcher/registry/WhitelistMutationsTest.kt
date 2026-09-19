package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistMutationsTest {
    @Test
    fun addAssignsOrderAndKeepsBioDefault() {
        val first = WhitelistMutations.add(emptyList(), "a")
        val second = WhitelistMutations.add(first, "b", bioEnabled = false)
        assertEquals(0, second[0].order)
        assertEquals(1, second[1].order)
        assertTrue(second[0].bioEnabled)
        assertFalse(second[1].bioEnabled)
    }

    @Test
    fun addDoesNotDuplicate() {
        val once = WhitelistMutations.add(emptyList(), "a")
        val twice = WhitelistMutations.add(once, "a")
        assertEquals(1, twice.size)
    }

    @Test
    fun removeRenormalizesOrder() {
        val entries = WhitelistMutations.addAll(emptyList(), listOf("a" to true, "b" to true, "c" to true))
        val removed = WhitelistMutations.remove(entries, "b")
        assertEquals(listOf("a", "c"), removed.map { it.packageName })
        assertEquals(listOf(0, 1), removed.map { it.order })
    }

    @Test
    fun moveChangesOrder() {
        val entries = WhitelistMutations.addAll(emptyList(), listOf("a" to true, "b" to true, "c" to true))
        val moved = WhitelistMutations.move(entries, 0, 2)
        assertEquals(listOf("b", "c", "a"), moved.map { it.packageName })
        assertEquals(listOf(0, 1, 2), moved.map { it.order })
    }

    @Test
    fun pruneOrphansNeverAdds() {
        val entries = WhitelistMutations.addAll(emptyList(), listOf("a" to true, "gone" to true))
        val pruned = WhitelistMutations.pruneOrphans(entries, setOf("a", "new.install"))
        assertEquals(listOf("a"), pruned.map { it.packageName })
        assertFalse(pruned.any { it.packageName == "new.install" })
    }
}
