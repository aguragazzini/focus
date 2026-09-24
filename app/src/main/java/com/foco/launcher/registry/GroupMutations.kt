package com.foco.launcher.registry

/**
 * Pure group edits. Callers pass the ids that are allowed in that section
 * (personal whitelist packages, or the current work-catalog keys).
 *
 * An app belongs to at most one group per section. Adding it to another group
 * in the same section moves it. The other section is left alone.
 * An empty group is deleted. Deleting a group does not touch the whitelist.
 */
object GroupMutations {
    const val NAME_MAX = 24

    fun normalizeName(raw: String): String? {
        val collapsed = raw.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return null
        return collapsed.take(NAME_MAX)
    }

    fun create(
        groups: List<AppGroup>,
        section: GroupSection,
        rawName: String,
        memberId: String,
        id: String,
        allowedIds: Set<String>,
    ): List<AppGroup> {
        val name = normalizeName(rawName) ?: return groups
        if (memberId.isBlank() || memberId !in allowedIds) return groups
        if (id.isBlank() || groups.any { it.id == id }) return groups
        val cleared = strip(groups, section, memberId)
        val order = (cleared.filter { it.section == section }.maxOfOrNull { it.order } ?: -1) + 1
        return normalizeOrders(
            cleared + AppGroup(
                id = id,
                section = section,
                name = name,
                order = order,
                members = listOf(memberId),
            ),
        )
    }

    fun addMember(
        groups: List<AppGroup>,
        groupId: String,
        memberId: String,
        allowedIds: Set<String>,
    ): List<AppGroup> {
        val target = groups.find { it.id == groupId } ?: return groups
        if (memberId.isBlank() || memberId !in allowedIds) return groups
        if (memberId in target.members) return groups
        val cleared = strip(groups, target.section, memberId)
        return normalizeOrders(
            cleared.map { group ->
                if (group.id == groupId) group.copy(members = group.members + memberId) else group
            },
        )
    }

    /** Drop one app. The last member deletes the group. */
    fun removeMember(groups: List<AppGroup>, groupId: String, memberId: String): List<AppGroup> {
        return normalizeOrders(
            groups.mapNotNull { group ->
                if (group.id != groupId) return@mapNotNull group
                val kept = group.members.filterNot { it == memberId }
                if (kept.isEmpty()) null else group.copy(members = kept)
            },
        )
    }

    fun rename(groups: List<AppGroup>, groupId: String, rawName: String): List<AppGroup> {
        val name = normalizeName(rawName) ?: return groups
        return groups.map { group ->
            if (group.id == groupId) group.copy(name = name) else group
        }
    }

    /** Apps return to the loose list because they are simply no longer members. */
    fun delete(groups: List<AppGroup>, groupId: String): List<AppGroup> {
        return normalizeOrders(groups.filterNot { it.id == groupId })
    }

    /**
     * Keep members that are still live in [section]. Other sections stay.
     * Used when a personal app leaves the whitelist or is uninstalled, and when
     * a work app or the whole work profile disappears.
     */
    fun retain(groups: List<AppGroup>, section: GroupSection, liveIds: Set<String>): List<AppGroup> {
        return normalizeOrders(
            groups.mapNotNull { group ->
                if (group.section != section) return@mapNotNull group
                val kept = group.members.filter { it in liveIds }.distinct()
                if (kept.isEmpty()) null else if (kept == group.members) group else group.copy(members = kept)
            },
        )
    }

    private fun strip(groups: List<AppGroup>, section: GroupSection, memberId: String): List<AppGroup> {
        return groups.mapNotNull { group ->
            if (group.section != section || memberId !in group.members) return@mapNotNull group
            val kept = group.members.filterNot { it == memberId }
            if (kept.isEmpty()) null else group.copy(members = kept)
        }
    }

    private fun normalizeOrders(groups: List<AppGroup>): List<AppGroup> {
        val rank = HashMap<String, Int>()
        for (section in GroupSection.entries) {
            groups.filter { it.section == section }
                .sortedWith(compareBy({ it.order }, { it.id }))
                .forEachIndexed { index, group -> rank[group.id] = index }
        }
        return groups.map { group ->
            val order = rank[group.id] ?: return@map group
            if (group.order == order) group else group.copy(order = order)
        }
    }
}

/** Whitelist replacement also drops personal-group members that are no longer listed. */
fun LauncherPrefs.withEntries(entries: List<WhitelistEntry>): LauncherPrefs {
    val allowed = entries.map { it.packageName }.toSet()
    val groups = GroupMutations.retain(this.groups, GroupSection.PERSONAL, allowed)
    if (entries == this.entries && groups == this.groups) return this
    return copy(entries = entries, groups = groups)
}
