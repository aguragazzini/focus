package com.foco.launcher.registry

import kotlinx.serialization.Serializable

/**
 * Home pager layout. An empty stored list means the default four pages.
 * Several pages may share a type: they read the same clock, whitelist, diet, or work catalog.
 * APPS is the personal grid without Pausar avisos. It is not a second whitelist.
 */
@Serializable
data class HomePageSpec(
    val id: String,
    val type: String,
    val label: String = "",
    val hidden: Boolean = false,
)

object HomePages {
    const val TYPE_CLOCK = "CLOCK"
    const val TYPE_PERSONAL = "PERSONAL"
    const val TYPE_DIET = "DIET"
    const val TYPE_WORK = "WORK"
    const val TYPE_APPS = "APPS"
    const val MAX = 12

    val TYPES = setOf(TYPE_CLOCK, TYPE_PERSONAL, TYPE_DIET, TYPE_WORK, TYPE_APPS)

    fun defaults(): List<HomePageSpec> = listOf(
        HomePageSpec(id = "page-clock", type = TYPE_CLOCK),
        HomePageSpec(id = "page-personal", type = TYPE_PERSONAL),
        HomePageSpec(id = "page-diet", type = TYPE_DIET),
        HomePageSpec(id = "page-work", type = TYPE_WORK),
    )

    /** Drop unknown rows. Empty, or nothing left visible, restores [defaults]. */
    fun resolve(raw: List<HomePageSpec>): List<HomePageSpec> {
        if (raw.isEmpty()) return defaults()
        val cleaned = ArrayList<HomePageSpec>(raw.size)
        val seen = HashSet<String>()
        for (spec in raw) {
            val type = spec.type.trim().uppercase()
            val id = spec.id.trim()
            if (type !in TYPES || id.isEmpty() || !seen.add(id)) continue
            val label = spec.label.trim().replace(Regex("\\s+"), " ").take(GroupMutations.NAME_MAX)
            cleaned += spec.copy(id = id, type = type, label = label)
        }
        if (cleaned.isEmpty() || cleaned.none { !it.hidden }) return defaults()
        return cleaned
    }

    fun visible(pages: List<HomePageSpec>): List<HomePageSpec> = pages.filter { !it.hidden }

    /** First visible Personal page. Otherwise the first visible page. */
    fun landingIndex(visible: List<HomePageSpec>): Int {
        if (visible.isEmpty()) return 0
        val personal = visible.indexOfFirst { it.type == TYPE_PERSONAL }
        return if (personal >= 0) personal else 0
    }

    fun create(pages: List<HomePageSpec>, type: String, label: String, id: String): List<HomePageSpec> {
        val kind = type.trim().uppercase()
        val pageId = id.trim()
        if (kind !in TYPES || pageId.isEmpty() || pages.any { it.id == pageId } || pages.size >= MAX) {
            return pages
        }
        val name = label.trim().replace(Regex("\\s+"), " ").take(GroupMutations.NAME_MAX)
        return pages + HomePageSpec(id = pageId, type = kind, label = name)
    }

    /**
     * A blank name clears the custom label so the type name shows again.
     * A non-blank name is trimmed and capped like a group name.
     */
    fun rename(pages: List<HomePageSpec>, id: String, raw: String): List<HomePageSpec> {
        if (pages.none { it.id == id }) return pages
        val collapsed = raw.trim().replace(Regex("\\s+"), " ")
        val name = if (collapsed.isEmpty()) "" else collapsed.take(GroupMutations.NAME_MAX)
        return pages.map { if (it.id == id) it.copy(label = name) else it }
    }

    fun move(pages: List<HomePageSpec>, id: String, delta: Int): List<HomePageSpec> {
        if (delta == 0) return pages
        val index = pages.indexOfFirst { it.id == id }
        if (index < 0) return pages
        val target = index + delta
        if (target !in pages.indices) return pages
        val next = pages.toMutableList()
        val item = next.removeAt(index)
        next.add(target, item)
        return next
    }

    fun setHidden(pages: List<HomePageSpec>, id: String, hidden: Boolean): List<HomePageSpec> {
        val page = pages.find { it.id == id } ?: return pages
        if (page.hidden == hidden) return pages
        if (hidden && pages.count { !it.hidden } <= 1) return pages
        return pages.map { if (it.id == id) it.copy(hidden = hidden) else it }
    }

    /** Refuses a delete that would leave no visible page. */
    fun delete(pages: List<HomePageSpec>, id: String): List<HomePageSpec> {
        if (pages.none { it.id == id }) return pages
        val remaining = pages.filter { it.id != id }
        if (remaining.none { !it.hidden }) return pages
        return remaining
    }

    fun canDelete(pages: List<HomePageSpec>, id: String): Boolean {
        return delete(pages, id) != pages
    }
}
