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
import kotlin.math.hypot

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

internal data class DragChrome(
    val section: GroupSection,
    val memberId: String,
    val fromGroupId: String?,
    val hoverKind: GroupDrag.HoverKind,
    val targetId: String?,
    val overlapArmed: Boolean,
)

internal data class GhostPose(
    val pointerX: Float,
    val pointerY: Float,
    val iconWidth: Float,
    val iconHeight: Float,
    val icon: Bitmap?,
    val label: String,
)

internal class HomeDragState {
    /** Highlight + which tile is lifted. Changes when the hover target changes, not per pixel. */
    var chrome by mutableStateOf<DragChrome?>(null)
        private set

    /** Pointer pose for the ghost. Only the ghost composable should read this. */
    var ghost by mutableStateOf<GhostPose?>(null)
        private set

    private var session: DragSession? = null

    val tiles = HashMap<String, GroupDrag.Tile>()
    val rows = HashMap<String, Pair<GroupSection, GroupDrag.Bounds>>()
    var folderPanel: GroupDrag.Bounds? = null

    private var hoverKey: String = ""
    private var hoverSince: Long = 0L
    private var tileSnapshot: List<GroupDrag.Tile> = emptyList()
    private var tileVersion = 0
    private var tileSnapshotVersion = -1
    private var gridSnapshot: Map<GroupSection, GroupDrag.Bounds> = emptyMap()
    private var gridVersion = 0
    private var gridSnapshotVersion = -1

    fun putTile(key: String, tile: GroupDrag.Tile) {
        val prev = tiles[key]
        if (prev == tile) return
        tiles[key] = tile
        tileVersion++
    }

    fun removeTile(key: String) {
        if (tiles.remove(key) != null) tileVersion++
    }

    fun putRow(key: String, section: GroupSection, bounds: GroupDrag.Bounds) {
        val prev = rows[key]
        if (prev != null && prev.first == section && nearly(prev.second, bounds)) return
        rows[key] = section to bounds
        gridVersion++
    }

    fun removeRow(key: String) {
        if (rows.remove(key) != null) gridVersion++
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
        chrome = DragChrome(
            section = section,
            memberId = memberId,
            fromGroupId = fromGroupId,
            hoverKind = GroupDrag.HoverKind.NONE,
            targetId = null,
            overlapArmed = false,
        )
        ghost = GhostPose(x, y, iconWidth, iconHeight, icon, label)
        move(x, y, nowMs)
    }

    fun move(windowX: Float, windowY: Float, nowMs: Long) {
        val current = session ?: return
        val dragged = GroupDrag.boundsCenteredOn(windowX, windowY, current.iconWidth, current.iconHeight)
        if (tileSnapshotVersion != tileVersion) {
            tileSnapshot = tiles.values.toList()
            tileSnapshotVersion = tileVersion
        }
        if (gridSnapshotVersion != gridVersion) {
            gridSnapshot = grids()
            gridSnapshotVersion = gridVersion
        }
        val raw = GroupDrag.evaluate(
            pointer = GroupDrag.Pointer(
                section = current.section,
                memberId = current.memberId,
                fromGroupId = current.fromGroupId,
                bounds = dragged,
                blockedTargetIds = current.blockedTargetIds,
            ),
            tiles = tileSnapshot,
            grids = gridSnapshot,
            gridBlock = if (current.fromGroupId != null) folderPanel else null,
        )
        val key = "${raw.kind}:${raw.targetId.orEmpty()}"
        if (key != hoverKey) {
            hoverKey = key
            hoverSince = nowMs
        }
        val hover = raw.copy(dwellMs = (nowMs - hoverSince).coerceAtLeast(0L))
        session = current.copy(
            pointerX = windowX,
            pointerY = windowY,
            hover = hover,
        )
        val armed = hover.overlap >= GroupDrag.MIN_OVERLAP
        val shown = chrome
        if (shown == null ||
            shown.hoverKind != hover.kind ||
            shown.targetId != hover.targetId ||
            shown.overlapArmed != armed
        ) {
            chrome = DragChrome(
                section = current.section,
                memberId = current.memberId,
                fromGroupId = current.fromGroupId,
                hoverKind = hover.kind,
                targetId = hover.targetId,
                overlapArmed = armed,
            )
        }
        val pose = ghost
        if (pose == null || pose.pointerX != windowX || pose.pointerY != windowY) {
            ghost = GhostPose(
                pointerX = windowX,
                pointerY = windowY,
                iconWidth = current.iconWidth,
                iconHeight = current.iconHeight,
                icon = current.icon,
                label = current.label,
            )
        }
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
        chrome = null
        ghost = null
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
    chrome: DragChrome?,
    cellId: String,
    cellGroupId: String?,
): CellHighlight {
    if (chrome == null) return CellHighlight.None
    val target = chrome.targetId ?: return CellHighlight.None
    val matchesFolder = cellGroupId != null && cellGroupId == target
    val matchesApp = cellGroupId == null && cellId == target
    if (!matchesFolder && !matchesApp) return CellHighlight.None
    if (chrome.hoverKind == GroupDrag.HoverKind.INVALID_CROSS) return CellHighlight.Invalid
    if (!chrome.overlapArmed) return CellHighlight.None
    return when (chrome.hoverKind) {
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

internal enum class DragPointerChoice { Wait, HoldStill, Arm, Drag, Yield, LongPress }

/**
 * A flick still leaves the tile so the pager or the list can move.
 * A slow horizontal drift is consumed so the pager does not steal the hold.
 * Past [DRAG_ARM_MS], movement becomes a group drag.
 */
internal fun dragPointerChoice(
    elapsedMs: Long,
    totalX: Float,
    totalY: Float,
    slop: Float,
    longPressMs: Long,
    armed: Boolean,
    wantsLongPress: Boolean,
): DragPointerChoice {
    val distance = hypot(totalX.toDouble(), totalY.toDouble()).toFloat()
    val wide = slop * 3f
    if (armed) {
        if (wantsLongPress && elapsedMs >= longPressMs && distance <= slop) {
            return DragPointerChoice.LongPress
        }
        return if (distance > slop) DragPointerChoice.Drag else DragPointerChoice.HoldStill
    }
    if (elapsedMs >= DRAG_ARM_MS && distance <= wide) {
        return if (distance > slop) DragPointerChoice.Drag else DragPointerChoice.Arm
    }
    if (distance > slop && elapsedMs < DRAG_ARM_MS) {
        if (abs(totalY) > abs(totalX)) return DragPointerChoice.Yield
        if (abs(totalX) > wide) return DragPointerChoice.Yield
        return DragPointerChoice.HoldStill
    }
    if (wantsLongPress && elapsedMs >= longPressMs && distance <= wide) {
        return DragPointerChoice.LongPress
    }
    return DragPointerChoice.Wait
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
            if (dragging) {
                change.consume()
                onDrag(change.position)
                continue
            }
            when (
                dragPointerChoice(
                    elapsedMs = elapsed,
                    totalX = total.x,
                    totalY = total.y,
                    slop = slop,
                    longPressMs = longPress,
                    armed = armed,
                    wantsLongPress = onLongClick != null,
                )
            ) {
                DragPointerChoice.Wait -> Unit
                DragPointerChoice.HoldStill -> change.consume()
                DragPointerChoice.Arm -> {
                    armed = true
                    change.consume()
                }
                DragPointerChoice.Drag -> {
                    armed = true
                    dragging = true
                    change.consume()
                    onDragStart(change.position)
                }
                DragPointerChoice.Yield -> break
                DragPointerChoice.LongPress -> {
                    change.consume()
                    onLongClick?.invoke()
                    while (true) {
                        val next = awaitPointerEvent()
                        val held = next.changes.firstOrNull { it.id == pointerId } ?: break
                        held.consume()
                        if (!held.pressed) break
                    }
                    break
                }
            }
        }
    }
}
