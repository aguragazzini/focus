package com.foco.launcher.registry

/**
 * Drag-to-group decisions. Callers supply geometry in one coordinate space.
 *
 * A drop onto another tile commits only when overlap is at least [MIN_OVERLAP]
 * and the pointer has dwelt on that target for [COMMIT_DWELL_MS] (inside the
 * 280–350 ms band). Personal and Trabajo never share a folder.
 * Dragging a member out of an open folder onto its own section grid ejects it.
 */
object GroupDrag {
    const val MIN_OVERLAP = 0.60f
    const val COMMIT_DWELL_MS = 320L

    /** Below a real hover, but close enough that the empty grid must not steal the drop. */
    const val AIM_OVERLAP = 0.35f

    data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        val width: Float get() = (right - left).coerceAtLeast(0f)
        val height: Float get() = (bottom - top).coerceAtLeast(0f)
        val area: Float get() = width * height
        val centerX: Float get() = (left + right) / 2f
        val centerY: Float get() = (top + bottom) / 2f

        fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
    }

    data class Tile(
        val section: GroupSection,
        val id: String,
        val folder: Boolean,
        val bounds: Bounds,
    )

    data class Pointer(
        val section: GroupSection,
        val memberId: String,
        val fromGroupId: String?,
        val bounds: Bounds,
        /** Other members of the open folder. They are not drop targets. */
        val blockedTargetIds: Set<String> = emptySet(),
    )

    enum class HoverKind {
        NONE,
        VALID_APP,
        VALID_FOLDER,
        VALID_GRID,
        INVALID_CROSS,
    }

    data class Hover(
        val kind: HoverKind = HoverKind.NONE,
        val targetId: String? = null,
        val overlap: Float = 0f,
        val dwellMs: Long = 0L,
    )

    enum class Action { SNAP_BACK, CREATE, ADD, REMOVE, REJECT_CROSS }

    fun overlapRatio(dragged: Bounds, target: Bounds): Float {
        val width = (minOf(dragged.right, target.right) - maxOf(dragged.left, target.left)).coerceAtLeast(0f)
        val height = (minOf(dragged.bottom, target.bottom) - maxOf(dragged.top, target.top)).coerceAtLeast(0f)
        val intersection = width * height
        val denom = minOf(dragged.area, target.area)
        if (denom <= 0f) return 0f
        return (intersection / denom).coerceIn(0f, 1f)
    }

    fun boundsCenteredOn(centerX: Float, centerY: Float, width: Float, height: Float): Bounds {
        val halfW = width / 2f
        val halfH = height / 2f
        return Bounds(centerX - halfW, centerY - halfH, centerX + halfW, centerY + halfH)
    }

    /**
     * @param gridBlock When the open folder panel contains the pointer, the
     * section grid underneath must not count as an eject target.
     */
    fun evaluate(
        pointer: Pointer,
        tiles: List<Tile>,
        grids: Map<GroupSection, Bounds>,
        gridBlock: Bounds? = null,
    ): Hover {
        val best = tiles
            .asSequence()
            .filter { tile ->
                if (tile.section == pointer.section && !tile.folder && tile.id == pointer.memberId) return@filter false
                if (pointer.fromGroupId != null && tile.folder && tile.id == pointer.fromGroupId) return@filter false
                if (!tile.folder && tile.id in pointer.blockedTargetIds) return@filter false
                true
            }
            .map { tile -> tile to overlapRatio(pointer.bounds, tile.bounds) }
            .filter { it.second > 0f }
            .maxByOrNull { it.second }

        if (best != null && best.second >= MIN_OVERLAP) {
            val tile = best.first
            if (tile.section != pointer.section) {
                return Hover(HoverKind.INVALID_CROSS, tile.id, best.second)
            }
            val kind = if (tile.folder) HoverKind.VALID_FOLDER else HoverKind.VALID_APP
            return Hover(kind, tile.id, best.second)
        }
        if (best != null && best.second >= AIM_OVERLAP) {
            val tile = best.first
            if (tile.section != pointer.section) {
                return Hover(HoverKind.INVALID_CROSS, tile.id, best.second)
            }
            return Hover(HoverKind.NONE, tile.id, best.second)
        }
        if (gridBlock != null && gridBlock.contains(pointer.bounds.centerX, pointer.bounds.centerY)) {
            return Hover(HoverKind.NONE)
        }
        val own = grids[pointer.section]
        if (own != null && own.contains(pointer.bounds.centerX, pointer.bounds.centerY)) {
            val overlap = overlapRatio(pointer.bounds, own)
            if (pointer.fromGroupId != null) {
                return Hover(HoverKind.VALID_GRID, pointer.fromGroupId, overlap)
            }
            return Hover(HoverKind.NONE, overlap = overlap)
        }
        val other = grids[other(pointer.section)]
        if (other != null && other.contains(pointer.bounds.centerX, pointer.bounds.centerY)) {
            return Hover(HoverKind.INVALID_CROSS, overlap = overlapRatio(pointer.bounds, other))
        }
        return Hover(HoverKind.NONE)
    }

    fun commit(hover: Hover): Action {
        if (hover.kind == HoverKind.INVALID_CROSS) return Action.REJECT_CROSS
        val armed = hover.dwellMs >= COMMIT_DWELL_MS && hover.overlap >= MIN_OVERLAP
        if (!armed) return Action.SNAP_BACK
        return when (hover.kind) {
            HoverKind.VALID_APP -> Action.CREATE
            HoverKind.VALID_FOLDER -> Action.ADD
            HoverKind.VALID_GRID -> if (hover.targetId != null) Action.REMOVE else Action.SNAP_BACK
            else -> Action.SNAP_BACK
        }
    }

    private fun other(section: GroupSection): GroupSection {
        return if (section == GroupSection.PERSONAL) GroupSection.WORK else GroupSection.PERSONAL
    }
}
