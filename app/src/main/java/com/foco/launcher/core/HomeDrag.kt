package com.foco.launcher.core

import android.graphics.Bitmap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import com.foco.launcher.registry.GroupDrag
import com.foco.launcher.registry.GroupSection
import kotlin.math.abs

/**
 * A short hold arms a tile drag so a flick still scrolls the home list.
 * Long-press without movement stays the backup sheet. Grouping itself still
 * needs overlap and dwell on the target ([GroupDrag.COMMIT_DWELL_MS]).
 */
internal const val DRAG_ARM_MS = 140L

internal enum class CellHighlight { None, Valid, Invalid }

internal data class DragSession(
    val section: GroupSection,
    val memberId: String,
    val label: String,
    val fromGroupId: String?,
    val blockedTargetIds: Set<String>,
    val pointerX: Float,
    val pointerY: Float,
    val iconWidth: Float,
    val iconHeight: Float,
    val hover: GroupDrag.Hover,
    val icon: Bitmap?,
)

internal class HomeDragState {
    var session by mutableStateOf<DragSession?>(null)
        private set

    val tiles = HashMap<String, GroupDrag.Tile>()
    val rows = HashMap<String, Pair<GroupSection, GroupDrag.Bounds>>()
    var folderPanel: GroupDrag.Bounds? = null

    private var hoverKey: String = ""
    private var hoverSince: Long = 0L

    fun putTile(key: String, tile: GroupDrag.Tile) {
        val prev = tiles[key]
        if (prev == tile) return
        tiles[key] = tile
    }

    fun removeTile(key: String) {
        tiles.remove(key)
    }

    fun putRow(key: String, section: GroupSection, bounds: GroupDrag.Bounds) {
        val prev = rows[key]
        if (prev != null && prev.first == section && nearly(prev.second, bounds)) return
        rows[key] = section to bounds
    }

    fun removeRow(key: String) {
        rows.remove(key)
    }

    fun start(
        section: GroupSection,
        memberId: String,
        label: String,
        fromGroupId: String?,
        blockedTargetIds: Set<String>,
        x: Float,
        y: Float,
        iconWidth: Float,
        iconHeight: Float,
        icon: Bitmap?,
        nowMs: Long,
    ) {
        hoverKey = ""
        hoverSince = nowMs
        session = DragSession(
            section = section,
            memberId = memberId,
            label = label,
            fromGroupId = fromGroupId,
            blockedTargetIds = blockedTargetIds,
            pointerX = x,
            pointerY = y,
            iconWidth = iconWidth,
            iconHeight = iconHeight,
            hover = GroupDrag.Hover(),
            icon = icon,
        )
        move(x, y, nowMs)
    }

    fun move(windowX: Float, windowY: Float, nowMs: Long) {
        val current = session ?: return
        val dragged = GroupDrag.boundsCenteredOn(windowX, windowY, current.iconWidth, current.iconHeight)
        val raw = GroupDrag.evaluate(
            pointer = GroupDrag.Pointer(
                section = current.section,
                memberId = current.memberId,
                fromGroupId = current.fromGroupId,
                bounds = dragged,
                blockedTargetIds = current.blockedTargetIds,
            ),
            tiles = tiles.values.toList(),
            grids = grids(),
            gridBlock = if (current.fromGroupId != null) folderPanel else null,
        )
        val key = "${raw.kind}:${raw.targetId.orEmpty()}"
        if (key != hoverKey) {
            hoverKey = key
            hoverSince = nowMs
        }
        session = current.copy(
            pointerX = windowX,
            pointerY = windowY,
            hover = raw.copy(dwellMs = (nowMs - hoverSince).coerceAtLeast(0L)),
        )
    }

    fun finish(): Pair<DragSession, GroupDrag.Action>? {
        val current = session ?: return null
        val action = GroupDrag.commit(current.hover)
        clear()
        return current to action
    }

    fun cancel() {
        clear()
    }

    private fun clear() {
        session = null
        hoverKey = ""
        hoverSince = 0L
    }

    private fun grids(): Map<GroupSection, GroupDrag.Bounds> {
        val out = HashMap<GroupSection, GroupDrag.Bounds>()
        for (section in GroupSection.entries) {
            val rects = rows.values.filter { it.first == section }.map { it.second }
            if (rects.isEmpty()) continue
            out[section] = GroupDrag.Bounds(
                left = rects.minOf { it.left },
                top = rects.minOf { it.top },
                right = rects.maxOf { it.right },
                bottom = rects.maxOf { it.bottom },
            )
        }
        return out
    }

    private fun nearly(a: GroupDrag.Bounds, b: GroupDrag.Bounds): Boolean {
        return abs(a.left - b.left) < 0.5f &&
            abs(a.top - b.top) < 0.5f &&
            abs(a.right - b.right) < 0.5f &&
            abs(a.bottom - b.bottom) < 0.5f
    }
}

internal fun highlightFor(
    session: DragSession?,
    cellId: String,
    cellGroupId: String?,
): CellHighlight {
    val hover = session?.hover ?: return CellHighlight.None
    val target = hover.targetId ?: return CellHighlight.None
    val matchesFolder = cellGroupId != null && cellGroupId == target
    val matchesApp = cellGroupId == null && cellId == target
    if (!matchesFolder && !matchesApp) return CellHighlight.None
    if (hover.kind == GroupDrag.HoverKind.INVALID_CROSS) return CellHighlight.Invalid
    if (hover.overlap < GroupDrag.MIN_OVERLAP) return CellHighlight.None
    return when (hover.kind) {
        GroupDrag.HoverKind.VALID_APP -> if (matchesApp) CellHighlight.Valid else CellHighlight.None
        GroupDrag.HoverKind.VALID_FOLDER -> if (matchesFolder) CellHighlight.Valid else CellHighlight.None
        else -> CellHighlight.None
    }
}

internal fun tileInitial(label: String): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return "·"
    val first = trimmed.first()
    return first.uppercaseChar().toString()
}

internal class CoordRef {
    var value: LayoutCoordinates? = null
}

internal fun LayoutCoordinates.toDragBounds(): GroupDrag.Bounds {
    val bounds = boundsInWindow()
    return GroupDrag.Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
}

internal fun Modifier.homeTileGesture(
    key: Any?,
    enabled: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = pointerInput(enabled, key) {
    if (!enabled) return@pointerInput
    val slop = viewConfiguration.touchSlop
    val longPress = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val pointerId = down.id
        var total = Offset.Zero
        var armed = false
        var dragging = false
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            if (!change.pressed) {
                if (dragging) onDragEnd() else if (total.getDistance() <= slop) onClick()
                break
            }
            total += change.position - change.previousPosition
            val elapsed = change.uptimeMillis - down.uptimeMillis
            if (!dragging) {
                if (!armed && elapsed >= DRAG_ARM_MS && total.getDistance() <= slop) {
                    armed = true
                }
                if (total.getDistance() > slop) {
                    if (!armed) break
                    dragging = true
                    change.consume()
                    onDragStart(change.position)
                } else if (onLongClick != null && elapsed >= longPress) {
                    change.consume()
                    onLongClick()
                    while (true) {
                        val next = awaitPointerEvent()
                        val held = next.changes.firstOrNull { it.id == pointerId } ?: break
                        held.consume()
                        if (!held.pressed) break
                    }
                    break
                }
            } else {
                change.consume()
                onDrag(change.position)
            }
        }
    }
}
