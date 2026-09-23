package com.foco.launcher.registry

object WhitelistMutations {
    fun add(
        entries: List<WhitelistEntry>,
        packageName: String,
        bioEnabled: Boolean = false,
        allowNotif: Boolean = true,
    ): List<WhitelistEntry> {
        if (entries.any { it.packageName == packageName }) return entries
        val order = (entries.maxOfOrNull { it.order } ?: -1) + 1
        return (entries + WhitelistEntry(packageName, order, bioEnabled, allowNotif)).normalized()
    }

    fun addAll(
        entries: List<WhitelistEntry>,
        packages: List<Pair<String, Boolean>>,
    ): List<WhitelistEntry> {
        var next = entries
        for ((pkg, bio) in packages) {
            next = add(next, pkg, bio)
        }
        return next
    }

    fun remove(entries: List<WhitelistEntry>, packageName: String): List<WhitelistEntry> {
        return entries.filterNot { it.packageName == packageName }.normalized()
    }

    fun move(entries: List<WhitelistEntry>, fromIndex: Int, toIndex: Int): List<WhitelistEntry> {
        val ordered = entries.sortedBy { it.order }
        if (fromIndex == toIndex) return ordered.normalized()
        if (fromIndex !in ordered.indices || toIndex !in ordered.indices) return ordered.normalized()
        val mutable = ordered.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return mutable.mapIndexed { index, entry -> entry.copy(order = index) }
    }

    fun pruneOrphans(entries: List<WhitelistEntry>, installed: Set<String>): List<WhitelistEntry> {
        return entries.filter { it.packageName in installed }.normalized()
    }

    fun List<WhitelistEntry>.normalized(): List<WhitelistEntry> {
        return sortedBy { it.order }.mapIndexed { index, entry -> entry.copy(order = index) }
    }
}
