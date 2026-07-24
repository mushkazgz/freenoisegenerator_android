package com.freenoisegenerator.app.ui.mainframe

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.freenoisegenerator.app.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun MainFrameScreen(
    isPlaying: Boolean,
    volume: Float,
    bassLevel: Float,
    maxBassLevel: Float,
    lowMidsLevel: Float,
    maxLowMidsLevel: Float,
    timerSteps: Int,
    maxTimerSteps: Int,
    timerDescription: String,
    onTogglePlayback: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit,
    onBassChange: (Float) -> Unit,
    onBassChangeFinished: () -> Unit,
    onLowMidsChange: (Float) -> Unit,
    onLowMidsChangeFinished: () -> Unit,
    onTimerChange: (Int) -> Unit,
    onTimerChangeFinished: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.TopStart
    ) {
        val density = LocalDensity.current
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()
        val isLandscape = screenWidthPx > screenHeightPx
        val imageScale = if (isLandscape) {
            min(
                screenHeightPx * LANDSCAPE_MACHINE_HEIGHT_FRACTION / MACHINE_HEIGHT,
                screenWidthPx * LANDSCAPE_MACHINE_WIDTH_FRACTION / MACHINE_WIDTH
            )
        } else {
            max(
                screenHeightPx / SOURCE_HEIGHT,
                screenWidthPx / SOURCE_VIEWPORT_WIDTH
            )
        }
        val imageOffsetX = if (isLandscape) {
            screenWidthPx / 2f - MACHINE_CENTER_X * imageScale
        } else {
            screenWidthPx / 2f - SOURCE_WIDTH / 2f * imageScale
        }
        val imageOffsetY = if (isLandscape) {
            screenHeightPx / 2f - MACHINE_CENTER_Y * imageScale
        } else {
            screenHeightPx * MACHINE_CENTER_SCREEN_FRACTION - MACHINE_CENTER_Y * imageScale
        }

        val backgroundBitmap = ImageBitmap.imageResource(R.drawable.mainframe_background)

        Canvas(Modifier.fillMaxSize()) {
            drawImage(
                image = backgroundBitmap,
                dstOffset = IntOffset(imageOffsetX.roundToInt(), imageOffsetY.roundToInt()),
                dstSize = IntSize(
                    (SOURCE_WIDTH * imageScale).roundToInt(),
                    (SOURCE_HEIGHT * imageScale).roundToInt()
                ),
                filterQuality = FilterQuality.High
            )
        }

        MainFrameKnob(
            imageRes = R.drawable.mainframe_knob_volume,
            label = "Volume",
            valueDescription = "${(volume * 100f).roundToInt()} percent",
            sourceCenter = Offset(520f, 718f),
            sourceVisualDiameter = 224f,
            sourceTouchDiameter = 270f,
            value = volume,
            valueRange = 0f..1f,
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onValueChange = onVolumeChange,
            onValueChangeFinished = onVolumeChangeFinished
        )
        MainFrameKnob(
            imageRes = R.drawable.mainframe_knob_bass,
            label = "Bass",
            valueDescription = "${(bassLevel / maxBassLevel * 100f).roundToInt()} percent",
            sourceCenter = Offset(318f, 979f),
            sourceVisualDiameter = 82f,
            sourceTouchDiameter = 120f,
            value = bassLevel,
            valueRange = 0f..maxBassLevel,
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onValueChange = onBassChange,
            onValueChangeFinished = onBassChangeFinished
        )
        MainFrameKnob(
            imageRes = R.drawable.mainframe_knob_low_mids,
            label = "Low mids",
            valueDescription = "${(lowMidsLevel / maxLowMidsLevel * 100f).roundToInt()} percent",
            sourceCenter = Offset(463f, 979f),
            sourceVisualDiameter = 82f,
            sourceTouchDiameter = 120f,
            value = lowMidsLevel,
            valueRange = 0f..maxLowMidsLevel,
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onValueChange = onLowMidsChange,
            onValueChangeFinished = onLowMidsChangeFinished
        )
        MainFrameKnob(
            imageRes = R.drawable.mainframe_knob_timer,
            label = "Timer",
            valueDescription = timerDescription,
            sourceCenter = Offset(679f, 984f),
            sourceVisualDiameter = 122f,
            sourceTouchDiameter = 155f,
            value = timerSteps.toFloat(),
            valueRange = 0f..maxTimerSteps.toFloat(),
            steps = maxTimerSteps - 1,
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onValueChange = { onTimerChange(it.roundToInt()) },
            onValueChangeFinished = onTimerChangeFinished
        )
        TimerReadout(
            text = timerDescription,
            sourceCenter = Offset(679f, 1053f),
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY)
        )

        PlaybackIndicator(
            isPlaying = isPlaying,
            sourceCenter = Offset(291f, 630f),
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onClick = onTogglePlayback
        )
        SettingsHotspot(
            sourceCenter = Offset(757f, 548f),
            imageScale = imageScale,
            imageOffset = Offset(imageOffsetX, imageOffsetY),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun MainFrameKnob(
    @DrawableRes imageRes: Int,
    label: String,
    valueDescription: String,
    sourceCenter: Offset,
    sourceVisualDiameter: Float,
    sourceTouchDiameter: Float,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    imageScale: Float,
    imageOffset: Offset,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    steps: Int = 0
) {
    val density = LocalDensity.current
    val visualDiameterPx = sourceVisualDiameter * imageScale
    val minimumTouchPx = with(density) { MINIMUM_TOUCH_SIZE.toPx() }
    val touchDiameterPx = max(sourceTouchDiameter * imageScale, minimumTouchPx)
    val centerX = imageOffset.x + sourceCenter.x * imageScale
    val centerY = imageOffset.y + sourceCenter.y * imageScale
    val visualDiameterDp = with(density) { visualDiameterPx.toDp() }
    val touchDiameterDp = with(density) { touchDiameterPx.toDp() }
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    var dragValue by remember { mutableFloatStateOf(value) }
    val fraction = ((value - valueRange.start) /
        (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    val targetRotation = MIN_ROTATION + ROTATION_SWEEP * fraction
    val animatedRotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "$label rotation"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - touchDiameterPx / 2f).roundToInt(),
                    (centerY - touchDiameterPx / 2f).roundToInt()
                )
            }
            .size(touchDiameterDp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = valueDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                setProgress { requestedValue ->
                    currentOnValueChange(requestedValue.coerceIn(valueRange))
                    currentOnValueChangeFinished()
                    true
                }
            }
            .pointerInput(valueRange) {
                detectDragGestures(
                    onDragStart = {
                        dragValue = currentValue
                    },
                    onDragEnd = { currentOnValueChangeFinished() },
                    onDragCancel = { currentOnValueChangeFinished() }
                ) { change, dragAmount ->
                    val valueDelta = -dragAmount.y / (touchDiameterPx * DRAG_DISTANCE_FACTOR) *
                        (valueRange.endInclusive - valueRange.start)
                    dragValue = (dragValue + valueDelta).coerceIn(valueRange)
                    currentOnValueChange(dragValue)
                    change.consume()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .size(visualDiameterDp)
                .graphicsLayer { rotationZ = animatedRotation }
        )
    }
}

@Composable
private fun TimerReadout(
    text: String,
    sourceCenter: Offset,
    imageScale: Float,
    imageOffset: Offset
) {
    val density = LocalDensity.current
    val widthPx = TIMER_READOUT_WIDTH * imageScale
    val heightPx = TIMER_READOUT_HEIGHT * imageScale
    val centerX = imageOffset.x + sourceCenter.x * imageScale
    val centerY = imageOffset.y + sourceCenter.y * imageScale
    val fontSize = with(density) { (TIMER_READOUT_TEXT_SIZE * imageScale).toSp() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - widthPx / 2f).roundToInt(),
                    (centerY - heightPx / 2f).roundToInt()
                )
            }
            .size(
                with(density) { widthPx.toDp() },
                with(density) { heightPx.toDp() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFE0B867),
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                shadow = Shadow(
                    color = Color.Black,
                    offset = Offset(0f, 1.5f * imageScale),
                    blurRadius = 1.5f * imageScale
                )
            )
        )
    }
}

@Composable
private fun PlaybackIndicator(
    isPlaying: Boolean,
    sourceCenter: Offset,
    imageScale: Float,
    imageOffset: Offset,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val touchDiameterPx = max(
        PLAYBACK_TOUCH_DIAMETER * imageScale,
        with(density) { MINIMUM_TOUCH_SIZE.toPx() }
    )
    val touchDiameterDp = with(density) { touchDiameterPx.toDp() }
    val centerX = imageOffset.x + sourceCenter.x * imageScale
    val centerY = imageOffset.y + sourceCenter.y * imageScale
    val pulseTransition = rememberInfiniteTransition(label = "Playback light")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Playback light pulse"
    )

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - touchDiameterPx / 2f).roundToInt(),
                    (centerY - touchDiameterPx / 2f).roundToInt()
                )
            }
            .size(touchDiameterDp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (isPlaying) "Pause noise" else "Start noise"
                stateDescription = if (isPlaying) "Playing" else "Paused"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(touchDiameterDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val sourceUnit = imageScale
            if (isPlaying) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xC8FFB342),
                            Color(0x48FF7A00),
                            Color.Transparent
                        ),
                        center = center,
                        radius = 28f * sourceUnit
                    ),
                    radius = 28f * sourceUnit,
                    center = center,
                    alpha = pulse
                )
            }
            drawCircle(
                color = Color(0xFF120C08),
                radius = 9f * sourceUnit,
                center = center
            )
            drawCircle(
                color = if (isPlaying) Color(0xFFFFB13B) else Color(0xFF5A3217),
                radius = 5.2f * sourceUnit,
                center = center,
                alpha = if (isPlaying) pulse else 1f
            )
            drawCircle(
                color = if (isPlaying) Color(0xFFFFE0A3) else Color(0xFF9C6230),
                radius = 2.2f * sourceUnit,
                center = center,
                alpha = if (isPlaying) pulse else 0.72f
            )
        }
    }
}

@Composable
private fun SettingsHotspot(
    sourceCenter: Offset,
    imageScale: Float,
    imageOffset: Offset,
    onClick: () -> Unit
) {
    val density = LocalDensity.current
    val touchDiameterPx = max(
        SETTINGS_TOUCH_DIAMETER * imageScale,
        with(density) { MINIMUM_TOUCH_SIZE.toPx() }
    )
    val touchDiameterDp = with(density) { touchDiameterPx.toDp() }
    val centerX = imageOffset.x + sourceCenter.x * imageScale
    val centerY = imageOffset.y + sourceCenter.y * imageScale

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (centerX - touchDiameterPx / 2f).roundToInt(),
                    (centerY - touchDiameterPx / 2f).roundToInt()
                )
            }
            .size(touchDiameterDp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "Settings"
            }
    )
}

private const val SOURCE_WIDTH = 1024f
private const val SOURCE_HEIGHT = 1535f
private const val SOURCE_VIEWPORT_WIDTH = 720f
private const val MACHINE_CENTER_X = 521f
private const val MACHINE_CENTER_Y = 792f
private const val MACHINE_WIDTH = 652f
private const val MACHINE_HEIGHT = 682f
private const val MACHINE_CENTER_SCREEN_FRACTION = 0.52f
private const val LANDSCAPE_MACHINE_WIDTH_FRACTION = 0.92f
private const val LANDSCAPE_MACHINE_HEIGHT_FRACTION = 0.94f
private const val MIN_ROTATION = -135f
private const val ROTATION_SWEEP = 270f
private const val PLAYBACK_TOUCH_DIAMETER = 80f
private const val SETTINGS_TOUCH_DIAMETER = 75f
private const val TIMER_READOUT_WIDTH = 112f
private const val TIMER_READOUT_HEIGHT = 24f
private const val TIMER_READOUT_TEXT_SIZE = 13f
private const val DRAG_DISTANCE_FACTOR = 1.35f
private val MINIMUM_TOUCH_SIZE = 48.dp
