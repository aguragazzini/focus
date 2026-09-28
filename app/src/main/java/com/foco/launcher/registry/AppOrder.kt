package com.foco.launcher.registry

import java.text.Collator
import java.util.Locale

/**
 * Spanish (Argentina) order for launcher lists the user sees.
 * Case is ignored. Accents stay secondary, and ñ stays its own letter.
 * Equal labels fall through to [tieBreak], which should be package or class.
 * Groups keep their stored order; this sorts apps, not folders.
 */
object AppOrder {
    private val collator: Collator = Collator.getInstance(Locale("es", "AR")).apply {
        strength = Collator.SECONDARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }

    fun compare(left: String, right: String): Int = synchronized(collator) {
        collator.compare(left, right)
    }

    fun <T> byLabel(
        items: List<T>,
        label: (T) -> String,
        tieBreak: (T) -> String = { "" },
    ): List<T> {
        return items.sortedWith { a, b ->
            val primary = compare(label(a), label(b))
            if (primary != 0) primary else tieBreak(a).compareTo(tieBreak(b))
        }
    }
}

/**
 * Personal whitelist. Custom up/down order wins once the user has set it.
 * Otherwise the visible list is [AppOrder], not the order apps were added.
 */
object WhitelistOrder {
    fun displayed(
        entries: List<WhitelistEntry>,
        customOrder: Boolean,
        labelOf: (String) -> String,
    ): List<WhitelistEntry> {
        if (customOrder) return entries.sortedBy { it.order }
        return AppOrder.byLabel(
            items = entries,
            label = { labelOf(it.packageName) },
            tieBreak = { it.packageName },
        )
    }
}
