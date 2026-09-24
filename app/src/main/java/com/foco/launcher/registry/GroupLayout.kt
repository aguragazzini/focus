package com.foco.launcher.registry

/**
 * How a home section is drawn.
 *
 * Groups come first, in ascending [AppGroup.order] (then id). Loose apps follow
 * in the section's existing order: whitelist order for Personal, catalog order
 * for Trabajo. An app that sits in a group is not repeated on the loose grid.
 * Members that are not in [orderedIds] are hidden so a stale id cannot draw a
 * ghost tile. Drag-reorder is not part of this version.
 *
 * Trabajo search is a separate path: a non-blank query hides group tiles and
 * lists every matching launchable, including apps that belong to a group.
 */
object GroupLayout {
    fun arrange(
        section: GroupSection,
        groups: List<AppGroup>,
        orderedIds: List<String>,
    ): ArrangedSection {
        val live = orderedIds.toSet()
        val shown = groups
            .filter { it.section == section }
            .sortedWith(compareBy({ it.order }, { it.id }))
            .mapNotNull { group ->
                val members = group.members.filter { it in live }.distinct()
                if (members.isEmpty()) null else if (members == group.members) group else group.copy(members = members)
            }
        val grouped = shown.flatMap { it.members }.toSet()
        return ArrangedSection(
            groups = shown,
            looseIds = orderedIds.filter { it !in grouped },
        )
    }
}

data class ArrangedSection(
    val groups: List<AppGroup>,
    val looseIds: List<String>,
)
