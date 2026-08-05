package com.freenoisegenerator.app.ui.settings

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.freenoisegenerator.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun WoodSettingsDialog(
    screensaverEnabled: Boolean,
    onScreensaverChange: (Boolean) -> Unit,
    onBluetoothClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val outsideInteraction = remember { MutableInteractionSource() }
        val panelInteraction = remember { MutableInteractionSource() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f))
                .clickable(
                    interactionSource = outsideInteraction,
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val panelWidth = minOf(
                    maxWidth * 0.88f,
                    maxHeight * 0.82f * PANEL_ASPECT_RATIO,
                    MAX_PANEL_WIDTH
                )
                val panelHeight = panelWidth / PANEL_ASPECT_RATIO

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(panelWidth, panelHeight)
                        .clickable(
                            interactionSource = panelInteraction,
                            indication = null,
                            onClick = {}
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.settings_panel),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "Settings",
                        color = LABEL_GOLD,
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 24.sp,
                            shadow = TEXT_SHADOW
                        ),
                        modifier = Modifier.offset(
                            x = panelWidth * 0.13f,
                            y = panelHeight * 0.105f
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = LABEL_GOLD,
                        modifier = Modifier
                            .offset(
                                x = panelWidth * 0.79f,
                                y = panelHeight * 0.105f
                            )
                            .size(panelWidth * 0.075f)
                    )
                    Text(
                        text = "Screensaver",
                        color = LABEL_GOLD.copy(alpha = 0.92f),
                        style = TextStyle(
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp,
                            shadow = TEXT_SHADOW
                        ),
                        modifier = Modifier.offset(
                            x = panelWidth * 0.14f,
                            y = panelHeight * 0.235f
                        )
                    )

                    WoodButton(
                        label = "On",
                        selected = screensaverEnabled,
                        width = panelWidth * SMALL_BUTTON_WIDTH,
                        height = panelHeight * BUTTON_HEIGHT,
                        onClick = { onScreensaverChange(true) },
                        modifier = Modifier.offset(
                            x = panelWidth * 0.16f,
                            y = panelHeight * 0.305f
                        )
                    )
                    WoodButton(
                        label = "Off",
                        selected = !screensaverEnabled,
                        width = panelWidth * SMALL_BUTTON_WIDTH,
                        height = panelHeight * BUTTON_HEIGHT,
                        onClick = { onScreensaverChange(false) },
                        modifier = Modifier.offset(
                            x = panelWidth * 0.56f,
                            y = panelHeight * 0.305f
                        )
                    )
                    WoodButton(
                        label = "Bluetooth",
                        icon = Icons.Default.Bluetooth,
                        wide = true,
                        width = panelWidth * WIDE_BUTTON_WIDTH,
                        height = panelHeight * BUTTON_HEIGHT,
                        onClick = onBluetoothClick,
                        modifier = Modifier.offset(
                            x = panelWidth * 0.20f,
                            y = panelHeight * 0.505f
                        )
                    )
                    WoodButton(
                        label = "Info",
                        width = panelWidth * SMALL_BUTTON_WIDTH,
                        height = panelHeight * BUTTON_HEIGHT,
                        onClick = onInfoClick,
                        modifier = Modifier.offset(
                            x = panelWidth * 0.16f,
                            y = panelHeight * 0.705f
                        )
                    )
                    WoodButton(
                        label = "Done",
                        width = panelWidth * SMALL_BUTTON_WIDTH,
                        height = panelHeight * BUTTON_HEIGHT,
                        onClick = onDismiss,
                        modifier = Modifier.offset(
                            x = panelWidth * 0.56f,
                            y = panelHeight * 0.705f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WoodButton(
    label: String,
    width: Dp,
    height: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    wide: Boolean = false,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    var activated by remember { mutableStateOf(false) }
    val engaged = pressed || activated
    val pressAmount by animateFloatAsState(
        targetValue = if (engaged) 1f else 0f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "$label press"
    )
    val lightAlpha by animateFloatAsState(
        targetValue = if (selected == true || engaged) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "$label indicator"
    )

    Box(
        modifier = modifier
            .size(width, height * 1.42f)
            .then(
                if (selected != null) {
                    Modifier.semantics { this.selected = selected }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Image(
            painter = painterResource(R.drawable.settings_indicator),
            contentDescription = null,
            modifier = Modifier
                .offset(y = height * 0.63f)
                .size(width * 0.58f, height * 0.72f)
                .graphicsLayer { alpha = lightAlpha }
        )
        Box(
            modifier = Modifier
                .size(width, height)
                .graphicsLayer {
                    scaleX = 1f - pressAmount * 0.018f
                    scaleY = 1f - pressAmount * 0.018f
                    translationY = pressAmount * 2.dp.toPx()
                }
                .clickable(
                    enabled = !activated,
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Button,
                    onClick = {
                        activated = true
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        scope.launch {
                            delay(ACTION_FLASH_MILLIS)
                            onClick()
                            activated = false
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    if (wide) {
                        R.drawable.settings_button_wide
                    } else {
                        R.drawable.settings_button_small
                    }
                ),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BUTTON_TEXT,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                }
                Text(
                    text = label,
                    color = BUTTON_TEXT,
                    style = TextStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        shadow = TEXT_SHADOW
                    )
                )
            }
        }
    }
}

private val LABEL_GOLD = Color(0xFFFFD27A)
private val BUTTON_TEXT = Color(0xFFFFE0A0)
private val TEXT_SHADOW = Shadow(color = Color.Black, blurRadius = 3f)
private val MAX_PANEL_WIDTH = 430.dp
private const val PANEL_ASPECT_RATIO = 768f / 755f
private const val SMALL_BUTTON_WIDTH = 0.28f
private const val WIDE_BUTTON_WIDTH = 0.60f
private const val BUTTON_HEIGHT = 0.115f
private const val ACTION_FLASH_MILLIS = 110L
