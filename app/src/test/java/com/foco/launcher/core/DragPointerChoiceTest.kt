package com.foco.launcher.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DragPointerChoiceTest {
    private val slop = 24f
    private val longPress = 400L

    @Test
    fun quickHorizontalFlickYieldsToThePager() {
        assertEquals(
            DragPointerChoice.Yield,
            dragPointerChoice(
                elapsedMs = 40,
                totalX = slop * 4,
                totalY = 4f,
                slop = slop,
                longPressMs = longPress,
                armed = false,
                wantsLongPress = true,
            ),
        )
    }

    @Test
    fun verticalMoveYieldsToTheList() {
        assertEquals(
            DragPointerChoice.Yield,
            dragPointerChoice(
                elapsedMs = 50,
                totalX = 4f,
                totalY = slop * 2,
                slop = slop,
                longPressMs = longPress,
                armed = false,
                wantsLongPress = true,
            ),
        )
    }

    @Test
    fun slowHorizontalDriftIsHeldUntilTheArm() {
        assertEquals(
            DragPointerChoice.HoldStill,
            dragPointerChoice(
                elapsedMs = 80,
                totalX = slop + 4f,
                totalY = 2f,
                slop = slop,
                longPressMs = longPress,
                armed = false,
                wantsLongPress = true,
            ),
        )
        assertEquals(
            DragPointerChoice.Drag,
            dragPointerChoice(
                elapsedMs = DRAG_ARM_MS,
                totalX = slop + 4f,
                totalY = 2f,
                slop = slop,
                longPressMs = longPress,
                armed = false,
                wantsLongPress = true,
            ),
        )
    }

    @Test
    fun stillHoldArmsThenLongPressWithoutMoving() {
        assertEquals(
            DragPointerChoice.Arm,
            dragPointerChoice(
                elapsedMs = DRAG_ARM_MS,
                totalX = 1f,
                totalY = 1f,
                slop = slop,
                longPressMs = longPress,
                armed = false,
                wantsLongPress = true,
            ),
        )
        assertEquals(
            DragPointerChoice.LongPress,
            dragPointerChoice(
                elapsedMs = longPress,
                totalX = 1f,
                totalY = 1f,
                slop = slop,
                longPressMs = longPress,
                armed = true,
                wantsLongPress = true,
            ),
        )
    }
}
