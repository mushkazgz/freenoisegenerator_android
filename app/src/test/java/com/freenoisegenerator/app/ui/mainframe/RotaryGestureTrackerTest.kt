package com.freenoisegenerator.app.ui.mainframe

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RotaryGestureTrackerTest {
    @Test
    fun clockwiseArcIncreasesValueLikeAPhysicalKnob() {
        val tracker = tracker()
        tracker.start(pointAt(-90f), CENTER, 0.20f)

        val result = listOf(-60f, -30f, 0f)
            .fold(0f) { _, angle -> tracker.dragTo(pointAt(angle)) }

        assertEquals(0.20f + 90f / ROTATION_SWEEP, result, 0.001f)
    }

    @Test
    fun counterClockwiseArcDecreasesValue() {
        val tracker = tracker()
        tracker.start(pointAt(0f), CENTER, 0.70f)

        val result = listOf(-30f, -60f, -90f)
            .fold(0f) { _, angle -> tracker.dragTo(pointAt(angle)) }

        assertEquals(0.70f - 90f / ROTATION_SWEEP, result, 0.001f)
    }

    @Test
    fun circularDragCrossesAngleBoundaryWithoutJumping() {
        val tracker = tracker()
        tracker.start(pointAt(170f), CENTER, 0.40f)

        val result = tracker.dragTo(pointAt(-170f))

        assertEquals(0.40f + 20f / ROTATION_SWEEP, result, 0.001f)
    }

    @Test
    fun horizontalDragThroughCenterDoesNotChangeValue() {
        val tracker = tracker()
        tracker.start(CENTER, CENTER, 0.20f)

        tracker.dragTo(CENTER + Offset(30f, 0f))
        val result = tracker.dragTo(CENTER + Offset(70f, 0f))

        assertEquals(0.20f, result, 0.001f)
    }

    @Test
    fun verticalDragThroughCenterDoesNotChangeValue() {
        val tracker = tracker()
        tracker.start(CENTER, CENTER, 0.20f)

        tracker.dragTo(CENTER + Offset(0f, -30f))
        val result = tracker.dragTo(CENTER + Offset(0f, -70f))

        assertEquals(0.20f, result, 0.001f)
    }

    @Test
    fun radialDragAtEdgeDoesNotChangeValue() {
        val tracker = tracker()
        tracker.start(Offset(170f, 100f), CENTER, 0.50f)

        val result = tracker.dragTo(Offset(140f, 100f))

        assertEquals(0.50f, result, 0.001f)
    }

    @Test
    fun movementNearCenterCannotCreateAnAngularJump() {
        val tracker = tracker()
        tracker.start(CENTER + Offset(5f, 0f), CENTER, 0.50f)

        val result = tracker.dragTo(CENTER + Offset(-5f, 0f))

        assertEquals(0.50f, result, 0.001f)
    }

    @Test
    fun physicalTurnRespectsValueStops() {
        val tracker = tracker()
        tracker.start(pointAt(-90f), CENTER, 0.95f)

        val maximum = listOf(-45f, 0f, 45f, 90f)
            .fold(0f) { _, angle -> tracker.dragTo(pointAt(angle)) }

        assertEquals(1f, maximum, 0f)
    }

    private fun tracker() = RotaryGestureTracker(
        touchDiameterPx = TOUCH_DIAMETER,
        rotationSweepDegrees = ROTATION_SWEEP
    )

    private fun pointAt(degrees: Float): Offset {
        val radians = degrees * PI.toFloat() / 180f
        return CENTER + Offset(cos(radians) * RADIUS, sin(radians) * RADIUS)
    }

    private companion object {
        val CENTER = Offset(100f, 100f)
        const val TOUCH_DIAMETER = 200f
        const val RADIUS = 70f
        const val ROTATION_SWEEP = 270f
    }
}
