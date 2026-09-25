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
     * ES-AR default folder label. Two app labels when we have them, otherwise
     * `Grupo`, `Grupo 2`, …
     */
    fun suggestedName(
        groups: List<AppGroup>,
        section: GroupSection,
        labelFirst: String,
        labelSecond: String,
    ): String {
        val joined = listOf(labelFirst, labelSecond)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
        val normalized = normalizeName(joined)
        if (normalized != null) return normalized
        val used = groups.filter { it.section == section }.map { it.name }.toSet()
        var index = 1
        while (index < 1000) {
            val candidate = if (index == 1) "Grupo" else "Grupo $index"
            if (candidate !in used) return candidate
            index++
        }
        return "Grupo"
    }

    /**
     * Drag app [draggedId] onto app [targetId] in the same section.
     * If the target already lives in a folder, the dragged app joins it.
     * Otherwise a new folder holds the target, then the dragged app.
     * A folder left with a single app by this drag dissolves. The whitelist
     * and the work catalog are not edited.
     */
    fun placeOnApp(
        groups: List<AppGroup>,
        section: GroupSection,
        draggedId: String,
        targetId: String,
        newGroupId: String,
        labelDragged: String,
        labelTarget: String,
        allowedIds: Set<String>,
    ): List<AppGroup> {
        if (draggedId.isBlank() || targetId.isBlank() || draggedId == targetId) return groups
        if (draggedId !in allowedIds || targetId !in allowedIds) return groups
        val touched = groups
            .filter { it.section == section && (draggedId in it.members || targetId in it.members) }
            .map { it.id }
            .toSet()
        val host = groups.find { it.section == section && targetId in it.members }
        if (host != null) {
            if (draggedId in host.members) return groups
            val added = addMember(groups, host.id, draggedId, allowedIds)
            return dissolveTouchedSingletons(added, touched - host.id)
        }
        if (newGroupId.isBlank() || groups.any { it.id == newGroupId }) return groups
        val name = suggestedName(groups, section, labelTarget, labelDragged)
        val cleared = strip(strip(groups, section, draggedId), section, targetId)
        val order = (cleared.filter { it.section == section }.maxOfOrNull { it.order } ?: -1) + 1
        val created = normalizeOrders(
            cleared + AppGroup(
                id = newGroupId,
                section = section,
                name = name,
                order = order,
                members = listOf(targetId, draggedId),
            ),
        )
        return dissolveTouchedSingletons(created, touched)
    }

    /**
     * Drag onto a closed or open folder. A source folder left with one app dissolves.
     */
    fun dragIntoFolder(
        groups: List<AppGroup>,
        groupId: String,
        memberId: String,
        allowedIds: Set<String>,
    ): List<AppGroup> {
        val target = groups.find { it.id == groupId } ?: return groups
        if (memberId in target.members) return groups
        val touched = groups
            .filter { it.section == target.section && memberId in it.members }
            .map { it.id }
            .toSet()
        val added = addMember(groups, groupId, memberId, allowedIds)
        if (added == groups) return groups
        return dissolveTouchedSingletons(added, touched - groupId)
    }

    /**
     * Drag a member onto the empty grid of its section.
     * One remaining app dissolves the folder. Apps stay launchable.
     */
    fun eject(groups: List<AppGroup>, groupId: String, memberId: String): List<AppGroup> {
        val group = groups.find { it.id == groupId } ?: return groups
        if (memberId !in group.members) return groups
        val kept = group.members.filterNot { it == memberId }
        if (kept.size < 2) return delete(groups, groupId)
        return removeMember(groups, groupId, memberId)
    }

    private fun dissolveTouchedSingletons(groups: List<AppGroup>, touchedIds: Set<String>): List<AppGroup> {
        if (touchedIds.isEmpty()) return groups
        val drop = groups.filter { it.id in touchedIds && it.members.size < 2 }.map { it.id }.toSet()
        if (drop.isEmpty()) return groups
        return normalizeOrders(groups.filterNot { it.id in drop })
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
