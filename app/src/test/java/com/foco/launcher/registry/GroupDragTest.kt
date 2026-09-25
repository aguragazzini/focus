package com.foco.launcher.registry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupDragTest {
    private val personalGrid = GroupDrag.Bounds(0f, 0f, 400f, 400f)
    private val workGrid = GroupDrag.Bounds(0f, 500f, 400f, 900f)

    private fun box(left: Float, top: Float, size: Float = 48f): GroupDrag.Bounds {
        return GroupDrag.Bounds(left, top, left + size, top + size)
    }

    private fun pointer(
        bounds: GroupDrag.Bounds,
        section: GroupSection = GroupSection.PERSONAL,
        memberId: String = "a",
        fromGroupId: String? = null,
        blocked: Set<String> = emptySet(),
    ): GroupDrag.Pointer {
        return GroupDrag.Pointer(section, memberId, fromGroupId, bounds, blocked)
    }

    @Test
    fun overlapUsesTheSmallerArea() {
        val dragged = box(0f, 0f)
        val target = box(10f, 0f)
        val ratio = GroupDrag.overlapRatio(dragged, target)
        assertTrue(ratio > 0.7f)
        assertTrue(ratio < 0.85f)
        assertEquals(0f, GroupDrag.overlapRatio(dragged, box(200f, 200f)))
    }

    @Test
    fun sixtyPercentAndDwellCreatesAGroup() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.PERSONAL, "b", folder = false, bounds = box(40f, 0f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(22f, 0f)),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.VALID_APP, hover.kind)
        assertEquals("b", hover.targetId)
        assertTrue(hover.overlap >= GroupDrag.MIN_OVERLAP)
        assertEquals(
            GroupDrag.Action.SNAP_BACK,
            GroupDrag.commit(hover.copy(dwellMs = GroupDrag.COMMIT_DWELL_MS - 1)),
        )
        assertEquals(
            GroupDrag.Action.CREATE,
            GroupDrag.commit(hover.copy(dwellMs = GroupDrag.COMMIT_DWELL_MS)),
        )
        assertEquals(
            GroupDrag.Action.CREATE,
            GroupDrag.commit(hover.copy(dwellMs = 350L)),
        )
    }

    @Test
    fun shallowOverlapDoesNotCommitWhileScrollingPast() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.PERSONAL, "b", folder = false, bounds = box(40f, 0f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(0f, 0f)),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.NONE, hover.kind)
        assertEquals(GroupDrag.Action.SNAP_BACK, GroupDrag.commit(hover.copy(dwellMs = 1_000L)))
    }

    @Test
    fun folderHoverAdds() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.PERSONAL, "g1", folder = true, bounds = box(10f, 10f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(12f, 12f), memberId = "a"),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.VALID_FOLDER, hover.kind)
        assertEquals(GroupDrag.Action.ADD, GroupDrag.commit(hover.copy(dwellMs = 320L)))
    }

    @Test
    fun crossSectionSnapsBack() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.WORK, "slack", folder = false, bounds = box(0f, 520f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(4f, 524f), section = GroupSection.PERSONAL, memberId = "phone"),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid, GroupSection.WORK to workGrid),
        )
        assertEquals(GroupDrag.HoverKind.INVALID_CROSS, hover.kind)
        assertEquals(GroupDrag.Action.REJECT_CROSS, GroupDrag.commit(hover.copy(dwellMs = 1_000L)))
    }

    @Test
    fun enteringTheOtherSectionGridRejectsEvenWithoutATile() {
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(20f, 700f)),
            tiles = emptyList(),
            grids = mapOf(GroupSection.PERSONAL to personalGrid, GroupSection.WORK to workGrid),
        )
        assertEquals(GroupDrag.HoverKind.INVALID_CROSS, hover.kind)
        assertEquals(GroupDrag.Action.REJECT_CROSS, GroupDrag.commit(hover))
    }

    @Test
    fun dragOutOntoOwnGridRemovesAfterDwell() {
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(20f, 40f), memberId = "a", fromGroupId = "g1"),
            tiles = emptyList(),
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.VALID_GRID, hover.kind)
        assertEquals("g1", hover.targetId)
        assertEquals(GroupDrag.Action.SNAP_BACK, GroupDrag.commit(hover.copy(dwellMs = 200L)))
        assertEquals(GroupDrag.Action.REMOVE, GroupDrag.commit(hover.copy(dwellMs = 320L)))
    }

    @Test
    fun looseAppOnEmptyGridDoesNotCreateOrRemove() {
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(20f, 40f), fromGroupId = null),
            tiles = emptyList(),
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.NONE, hover.kind)
        assertEquals(GroupDrag.Action.SNAP_BACK, GroupDrag.commit(hover.copy(dwellMs = 1_000L)))
    }

    @Test
    fun openFolderPanelBlocksAnAccidentalEject() {
        val panel = GroupDrag.Bounds(40f, 200f, 360f, 380f)
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(80f, 240f), memberId = "a", fromGroupId = "g1"),
            tiles = emptyList(),
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
            gridBlock = panel,
        )
        assertEquals(GroupDrag.HoverKind.NONE, hover.kind)
        assertEquals(GroupDrag.Action.SNAP_BACK, GroupDrag.commit(hover.copy(dwellMs = 1_000L)))
    }

    @Test
    fun siblingsInsideTheOpenFolderAreNotTargets() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.PERSONAL, "b", folder = false, bounds = box(10f, 10f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(12f, 12f), memberId = "a", fromGroupId = "g1", blocked = setOf("b")),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
            gridBlock = GroupDrag.Bounds(0f, 0f, 100f, 100f),
        )
        assertEquals(GroupDrag.HoverKind.NONE, hover.kind)
    }

    @Test
    fun aimingAtATileDoesNotFallThroughToGridEject() {
        val tiles = listOf(
            GroupDrag.Tile(GroupSection.PERSONAL, "b", folder = false, bounds = box(30f, 0f)),
        )
        val hover = GroupDrag.evaluate(
            pointer = pointer(box(0f, 0f), memberId = "a", fromGroupId = "g1"),
            tiles = tiles,
            grids = mapOf(GroupSection.PERSONAL to personalGrid),
        )
        assertEquals(GroupDrag.HoverKind.NONE, hover.kind)
        assertEquals(GroupDrag.Action.SNAP_BACK, GroupDrag.commit(hover.copy(dwellMs = 1_000L)))
    }
}
