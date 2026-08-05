package com.freenoisegenerator.app.ui.mainframe

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

/** Converts a physical clockwise or counter-clockwise turn into a value fraction. */
internal class RotaryGestureTracker(
    private val touchDiameterPx: Float,
    rotationSweepDegrees: Float,
    private val minimumAngularRadiusFraction: Float = 0.10f
) {
    private val rotationSweepRadians = rotationSweepDegrees * PI.toFloat() / 180f
    private var center = Offset.Zero
    private var previousPosition = Offset.Zero
    private var valueFraction = 0f

    fun start(position: Offset, center: Offset, initialFraction: Float) {
        this.center = center
        previousPosition = position
        valueFraction = initialFraction.coerceIn(0f, 1f)
    }

    fun dragTo(position: Offset): Float {
        val previousVector = previousPosition - center
        val currentVector = position - center
        val previousRadius = previousVector.getDistance()
        val currentRadius = currentVector.getDistance()
        val minimumAngularRadius = touchDiameterPx * minimumAngularRadiusFraction
        val fractionDelta = if (min(previousRadius, currentRadius) < minimumAngularRadius) {
            0f
        } else {
            shortestAngleDelta(previousVector, currentVector) / rotationSweepRadians
        }

        valueFraction = (valueFraction + fractionDelta).coerceIn(0f, 1f)
        previousPosition = position
        return valueFraction
    }
}

private fun shortestAngleDelta(previousVector: Offset, currentVector: Offset): Float {
    val previousAngle = atan2(previousVector.y, previousVector.x)
    val currentAngle = atan2(currentVector.y, currentVector.x)
    var delta = currentAngle - previousAngle
    val fullTurn = (2f * PI).toFloat()
    val halfTurn = PI.toFloat()
    while (delta > halfTurn) delta -= fullTurn
    while (delta < -halfTurn) delta += fullTurn
    return delta
}
