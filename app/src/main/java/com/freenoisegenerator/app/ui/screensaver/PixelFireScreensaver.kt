package com.freenoisegenerator.app.ui.screensaver

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.freenoisegenerator.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
internal fun PixelFireScreensaver(modifier: Modifier = Modifier) {
    val bonfireImage = ImageBitmap.imageResource(R.drawable.pixelart_bonfire)
    val fire = remember { PixelFireSimulation() }
    var animationFrame by remember { mutableIntStateOf(0) }

    LaunchedEffect(fire) {
        while (isActive) {
            fire.step()
            animationFrame += 1
            delay(FRAME_DELAY_MILLIS)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        animationFrame
        drawRect(color = Color.Black)

        val imageScale = min(
            size.width * IMAGE_WIDTH_MULTIPLIER / bonfireImage.width,
            size.height * MAX_IMAGE_HEIGHT_FRACTION / bonfireImage.height
        )
        val imageWidth = (bonfireImage.width * imageScale).roundToInt()
        val imageHeight = (bonfireImage.height * imageScale).roundToInt()
        val imageLeft = ((size.width - imageWidth) / 2f).roundToInt()
        val imageTop = (size.height * IMAGE_BOTTOM_FRACTION - imageHeight).roundToInt()

        drawImage(
            image = bonfireImage,
            dstOffset = IntOffset(imageLeft, imageTop),
            dstSize = IntSize(imageWidth, imageHeight),
            filterQuality = FilterQuality.None
        )

        val firePixelSize = floor(imageWidth / FIRE_SCALE_REFERENCE_WIDTH).coerceAtLeast(2f)
        val fireCenterX = imageLeft + imageWidth * 0.5f
        val fireBottomY = imageTop + imageHeight * FIRE_BASE_Y_FRACTION
        drawFireGrid(fire, fireCenterX, fireBottomY, firePixelSize)
        drawEmbers(fire, fireCenterX, fireBottomY, firePixelSize)
    }
}

private fun DrawScope.drawFireGrid(
    fire: PixelFireSimulation,
    centerX: Float,
    bottomY: Float,
    pixelSize: Float
) {
    val left = centerX - fire.width * pixelSize / 2f
    val top = bottomY - fire.height * pixelSize

    for (y in 0 until fire.height - HIDDEN_SOURCE_ROWS) {
        for (x in 0 until fire.width) {
            val heat = fire.heatAt(x, y)
            val paletteIndex = heat * FIRE_PALETTE.lastIndex / MAX_HEAT
            if (paletteIndex <= 1) continue
            val heatAlpha = ((heat - MIN_VISIBLE_HEAT).toFloat() /
                (MAX_HEAT - MIN_VISIBLE_HEAT)).coerceIn(0.18f, 1f)
            drawRect(
                color = FIRE_PALETTE[paletteIndex],
                topLeft = Offset(
                    x = floor(left + x * pixelSize),
                    y = floor(top + y * pixelSize)
                ),
                size = Size(pixelSize, pixelSize),
                alpha = FIRE_OVERLAY_ALPHA * heatAlpha,
                blendMode = BlendMode.Screen
            )
        }
    }
}

private fun DrawScope.drawEmbers(
    fire: PixelFireSimulation,
    centerX: Float,
    bottomY: Float,
    pixelSize: Float
) {
    val left = centerX - fire.width * pixelSize / 2f
    val top = bottomY - fire.height * pixelSize

    fire.forEachEmber { x, y, life, maxLife ->
        val remaining = life.toFloat() / maxLife
        val color = when {
            remaining > 0.66f -> EmberColors.Bright
            remaining > 0.3f -> EmberColors.Warm
            else -> EmberColors.Dim
        }
        drawRect(
            color = color,
            topLeft = Offset(
                x = floor(left + x * pixelSize),
                y = floor(top + y * pixelSize)
            ),
            size = Size(pixelSize, pixelSize),
            alpha = EMBER_OVERLAY_ALPHA,
            blendMode = BlendMode.Screen
        )
    }
}

private class PixelFireSimulation(
    val width: Int = FIRE_WIDTH,
    val height: Int = FIRE_HEIGHT,
    seed: Int = 0xF1A6E
) {
    private val random = Random(seed)
    private val heat = IntArray(width * height)
    private val emberX = FloatArray(MAX_EMBERS)
    private val emberY = FloatArray(MAX_EMBERS)
    private val emberDrift = FloatArray(MAX_EMBERS)
    private val emberRise = FloatArray(MAX_EMBERS)
    private val emberLife = IntArray(MAX_EMBERS)
    private val emberMaxLife = IntArray(MAX_EMBERS)
    private var frame = 0

    init {
        repeat(PREWARM_STEPS) {
            seedFire()
            propagateHeat()
        }
    }

    fun heatAt(x: Int, y: Int): Int = heat[y * width + x]

    fun step() {
        seedFire()
        propagateHeat()
        updateEmbers()
        frame += 1
    }

    fun forEachEmber(block: (x: Float, y: Float, life: Int, maxLife: Int) -> Unit) {
        for (index in 0 until MAX_EMBERS) {
            if (emberLife[index] > 0) {
                block(emberX[index], emberY[index], emberLife[index], emberMaxLife[index])
            }
        }
    }

    private fun seedFire() {
        val center = width / 2f
        val activeRadius = width * 0.34f
        for (sourceRow in height - SOURCE_ROWS until height) {
            for (x in 0 until width) {
                val distance = abs(x - center)
                heat[sourceRow * width + x] = when {
                    distance > activeRadius -> 0
                    distance > activeRadius * 0.82f -> random.nextInt(90, 190)
                    random.nextInt(100) < 7 -> random.nextInt(130, 205)
                    else -> random.nextInt(220, MAX_HEAT + 1)
                }
            }
        }
    }

    private fun propagateHeat() {
        for (y in 0 until height - SOURCE_ROWS) {
            for (x in 0 until width) {
                val sampleX = (x + random.nextInt(-2, 3)).coerceIn(0, width - 1)
                val first = heat[(y + 1) * width + sampleX]
                val second = heat[(y + 2).coerceAtMost(height - 1) * width + x]
                val mixed = (first * 3 + second) / 4
                val edgeCooling = if (x < 5 || x >= width - 5) 5 else 0
                heat[y * width + x] =
                    (mixed - random.nextInt(0, 6) - edgeCooling).coerceAtLeast(0)
            }
        }
    }

    private fun updateEmbers() {
        for (index in 0 until MAX_EMBERS) {
            if (emberLife[index] <= 0) continue
            emberX[index] += emberDrift[index]
            emberY[index] -= emberRise[index]
            emberDrift[index] += random.nextFloat() * 0.08f - 0.04f
            emberLife[index] -= 1
        }

        if (frame % 2 == 0 && random.nextFloat() < 0.72f) {
            val slot = emberLife.indexOfFirst { it <= 0 }
            if (slot >= 0) {
                emberX[slot] = width / 2f + random.nextInt(-12, 13)
                emberY[slot] = height - random.nextInt(13, 23).toFloat()
                emberDrift[slot] = random.nextFloat() * 0.46f - 0.23f
                emberRise[slot] = random.nextFloat() * 0.75f + 0.65f
                emberMaxLife[slot] = random.nextInt(18, 39)
                emberLife[slot] = emberMaxLife[slot]
            }
        }
    }
}

private object EmberColors {
    val Dim = Color(0xFF7D1E08)
    val Warm = Color(0xFFE34B0B)
    val Bright = Color(0xFFFFC247)
}

private val FIRE_PALETTE = arrayOf(
    Color(0x00000000),
    Color(0xFF120201),
    Color(0xFF240301),
    Color(0xFF3A0501),
    Color(0xFF540801),
    Color(0xFF701001),
    Color(0xFF8D1902),
    Color(0xFFAA2503),
    Color(0xFFC73305),
    Color(0xFFE04407),
    Color(0xFFF05A09),
    Color(0xFFFF7410),
    Color(0xFFFF9220),
    Color(0xFFFFB43A),
    Color(0xFFFFD968),
    Color(0xFFFFF1AF)
)

private const val IMAGE_WIDTH_MULTIPLIER = 1.28f
private const val MAX_IMAGE_HEIGHT_FRACTION = 0.86f
private const val IMAGE_BOTTOM_FRACTION = 0.88f
private const val FIRE_BASE_Y_FRACTION = 0.565f
private const val FIRE_SCALE_REFERENCE_WIDTH = 120f
private const val FIRE_WIDTH = 44
private const val FIRE_HEIGHT = 54
private const val SOURCE_ROWS = 3
private const val HIDDEN_SOURCE_ROWS = 8
private const val MIN_VISIBLE_HEAT = 32
private const val MAX_HEAT = 255
private const val MAX_EMBERS = 18
private const val PREWARM_STEPS = FIRE_HEIGHT * 2
private const val FIRE_OVERLAY_ALPHA = 0.70f
private const val EMBER_OVERLAY_ALPHA = 0.86f
private const val FRAME_DELAY_MILLIS = 72L
