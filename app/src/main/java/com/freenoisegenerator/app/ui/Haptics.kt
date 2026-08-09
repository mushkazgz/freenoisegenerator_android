package com.freenoisegenerator.app.ui

import android.view.HapticFeedbackConstants
import android.view.View

internal fun View.performControlHaptic(emphasized: Boolean = false) {
    val performed = performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    if (emphasized && performed) {
        postDelayed(
            { performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
            EMPHASIZED_HAPTIC_DELAY_MILLIS
        )
    }
}

private const val EMPHASIZED_HAPTIC_DELAY_MILLIS = 55L
