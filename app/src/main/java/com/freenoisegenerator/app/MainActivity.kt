package com.freenoisegenerator.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.freenoisegenerator.app.service.NoiseService
import com.freenoisegenerator.app.ui.theme.FreeNoiseGeneratorTheme
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        setContent {
            FreeNoiseGeneratorTheme {
                NoiseApp()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
    }
}

@Composable
private fun NoiseApp() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("noise_settings", Context.MODE_PRIVATE)
    }
    var isPlaying by remember { mutableStateOf(false) }
    var volume by remember {
        mutableStateOf(preferences.getFloat(KEY_VOLUME, DEFAULT_VOLUME))
    }
    var bassLevel by remember {
        mutableStateOf(preferences.getFloat(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL).coerceIn(0f, MAX_BAND_LEVEL))
    }
    var lowMidsLevel by remember {
        mutableStateOf(
            preferences.getFloat(KEY_LOW_MIDS_LEVEL, DEFAULT_LOW_MIDS_LEVEL)
                .coerceIn(0f, MAX_LOW_MIDS_LEVEL)
        )
    }
    var timerSteps by remember {
        mutableStateOf(preferences.getInt(KEY_TIMER_STEPS, DEFAULT_TIMER_STEPS).coerceIn(0, MAX_TIMER_STEPS))
    }
    val timerHandler = remember { Handler(Looper.getMainLooper()) }
    var pendingTimerRunnable by remember { mutableStateOf<Runnable?>(null) }
    var timerEndsAtMillis by remember { mutableStateOf(0L) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var lastInteractionMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var isDimmed by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var screensaverEnabled by remember {
        mutableStateOf(preferences.getBoolean(KEY_SCREENSAVER_ENABLED, true))
    }

    fun registerInteraction() {
        lastInteractionMillis = System.currentTimeMillis()
        isDimmed = false
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == NoiseService.ACTION_STATE_CHANGED) {
                    isPlaying = intent.getBooleanExtra(NoiseService.EXTRA_IS_PLAYING, false)
                    timerEndsAtMillis = intent.getLongExtra(NoiseService.EXTRA_TIMER_ENDS_AT_MILLIS, 0L)
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(NoiseService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            pendingTimerRunnable?.let(timerHandler::removeCallbacks)
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    LaunchedEffect(timerEndsAtMillis, isPlaying) {
        while (isPlaying && timerEndsAtMillis > 0L) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000L)
        }
    }

    LaunchedEffect(lastInteractionMillis, screensaverEnabled) {
        if (!screensaverEnabled) {
            isDimmed = false
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(IDLE_DIM_DELAY_MILLIS)
        if (System.currentTimeMillis() - lastInteractionMillis >= IDLE_DIM_DELAY_MILLIS) {
            isDimmed = true
        }
    }

    fun saveVolume() {
        preferences.edit().putFloat(KEY_VOLUME, volume).apply()
    }

    fun saveBandLevels() {
        preferences.edit()
            .putFloat(KEY_BASS_LEVEL, bassLevel.coerceIn(0f, MAX_BAND_LEVEL))
            .putFloat(KEY_LOW_MIDS_LEVEL, lowMidsLevel.coerceIn(0f, MAX_LOW_MIDS_LEVEL))
            .apply()
    }

    fun saveTimer() {
        preferences.edit().putInt(KEY_TIMER_STEPS, timerSteps.coerceIn(0, MAX_TIMER_STEPS)).apply()
    }

    fun setScreensaverEnabled(enabled: Boolean) {
        screensaverEnabled = enabled
        preferences.edit().putBoolean(KEY_SCREENSAVER_ENABLED, enabled).apply()
        if (!enabled) {
            isDimmed = false
        } else {
            registerInteraction()
        }
    }

    fun startPlayback() {
        context.startNoisePlayback(volume, bassLevel, lowMidsLevel, timerSteps.toTimerMillis())
    }

    fun play() {
        saveVolume()
        saveBandLevels()
        saveTimer()
        startPlayback()
        isPlaying = true
    }

    fun stop() {
        context.stopNoisePlayback()
        isPlaying = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        registerInteraction()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Free Noise Generator",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (isPlaying) "Playing now" else "Ready when you are",
                color = AppColors.TextMuted,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(34.dp))
            ElevatedButton(
                onClick = { if (isPlaying) stop() else play() },
                modifier = Modifier.size(176.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, if (isPlaying) AppColors.ButtonBorderActive else AppColors.ButtonBorder),
                elevation = ButtonDefaults.elevatedButtonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                ),
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = if (isPlaying) AppColors.ButtonActive else AppColors.ButtonIdle,
                    contentColor = if (isPlaying) AppColors.TextPrimary else AppColors.ButtonIdleText
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isPlaying) "Pause noise" else "Start noise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(34.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = AppColors.Panel,
                border = BorderStroke(1.dp, AppColors.PanelBorder)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Volume",
                            color = AppColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(volume * 100).toInt()}%",
                            color = AppColors.TextMuted,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                registerInteraction()
                                showSettingsDialog = true
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            onValueChangeFinished = {
                                saveVolume()
                                if (isPlaying) startPlayback()
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = AppColors.Accent,
                                activeTrackColor = AppColors.Accent,
                                inactiveTrackColor = AppColors.Track,
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = AppColors.TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    BandSlider(
                        name = "Bass",
                        value = bassLevel,
                        onValueChange = { bassLevel = it },
                        onValueChangeFinished = {
                            saveBandLevels()
                            if (isPlaying) {
                                startPlayback()
                            }
                        }
                    )
                    BandSlider(
                        name = "Low mids",
                        value = lowMidsLevel,
                        maxValue = MAX_LOW_MIDS_LEVEL,
                        onValueChange = { lowMidsLevel = it },
                        onValueChangeFinished = {
                            saveBandLevels()
                            if (isPlaying) {
                                startPlayback()
                            }
                        }
                    )
                    TimerSlider(
                        steps = timerSteps,
                        timerEndsAtMillis = timerEndsAtMillis,
                        nowMillis = nowMillis,
                        onValueChange = {
                            timerSteps = it
                            timerEndsAtMillis = 0L
                            pendingTimerRunnable?.let(timerHandler::removeCallbacks)
                            pendingTimerRunnable = null
                        },
                        onValueChangeFinished = {
                            saveTimer()
                            pendingTimerRunnable?.let(timerHandler::removeCallbacks)
                            val runnable = Runnable {
                                if (isPlaying) {
                                    startPlayback()
                                }
                                nowMillis = System.currentTimeMillis()
                                pendingTimerRunnable = null
                            }
                            pendingTimerRunnable = runnable
                            timerHandler.postDelayed(runnable, TIMER_APPLY_DELAY_MILLIS)
                        }
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = isDimmed,
            enter = fadeIn(animationSpec = tween(durationMillis = 1_600)),
            exit = fadeOut(animationSpec = tween(durationMillis = 450))
        ) {
            IdleRestOverlay()
        }
        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
        if (showSettingsDialog) {
            SettingsDialog(
                screensaverEnabled = screensaverEnabled,
                onScreensaverChange = ::setScreensaverEnabled,
                onInfoClick = {
                    showSettingsDialog = false
                    showInfoDialog = true
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    screensaverEnabled: Boolean,
    onScreensaverChange: (Boolean) -> Unit,
    onInfoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextMuted,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = AppColors.Accent)
            }
        },
        title = {
            Text(
                text = "Settings",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Screensaver")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ElevatedButton(
                        onClick = { onScreensaverChange(true) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (screensaverEnabled) AppColors.ButtonIdle else AppColors.ButtonActive,
                            contentColor = if (screensaverEnabled) AppColors.ButtonIdleText else AppColors.TextPrimary
                        )
                    ) {
                        Text(text = "On")
                    }
                    ElevatedButton(
                        onClick = { onScreensaverChange(false) },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = if (!screensaverEnabled) AppColors.ButtonIdle else AppColors.ButtonActive,
                            contentColor = if (!screensaverEnabled) AppColors.ButtonIdleText else AppColors.TextPrimary
                        )
                    ) {
                        Text(text = "Off")
                    }
                }
                TextButton(onClick = onInfoClick) {
                    Text(text = "Info", color = AppColors.Accent)
                }
            }
        }
    )
}

@Composable
private fun IdleRestOverlay() {
    val transition = rememberInfiniteTransition(label = "Idle ember rest")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ember drift"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4_800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Ember pulse"
    )
    val shootingStar by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shooting star"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.IdleOverlay)
    ) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    AppColors.NightSky.copy(alpha = 0.46f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = size.height * 0.52f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.MilkyWayViolet.copy(alpha = 0.18f),
                    AppColors.MilkyWayBlue.copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.38f, size.height * 0.18f),
                radius = size.width * 0.72f
            ),
            radius = size.width * 0.72f,
            center = Offset(size.width * 0.38f, size.height * 0.18f)
        )

        val constellations = listOf(
            listOf(
                Offset(size.width * 0.16f, size.height * 0.10f),
                Offset(size.width * 0.22f, size.height * 0.16f),
                Offset(size.width * 0.30f, size.height * 0.13f),
                Offset(size.width * 0.36f, size.height * 0.21f)
            ),
            listOf(
                Offset(size.width * 0.58f, size.height * 0.08f),
                Offset(size.width * 0.65f, size.height * 0.14f),
                Offset(size.width * 0.72f, size.height * 0.12f),
                Offset(size.width * 0.78f, size.height * 0.19f),
                Offset(size.width * 0.69f, size.height * 0.24f)
            ),
            listOf(
                Offset(size.width * 0.18f, size.height * 0.32f),
                Offset(size.width * 0.27f, size.height * 0.29f),
                Offset(size.width * 0.33f, size.height * 0.36f),
                Offset(size.width * 0.43f, size.height * 0.34f)
            )
        )

        constellations.forEach { stars ->
            stars.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = AppColors.StarTrail.copy(alpha = 0.18f),
                    start = start,
                    end = end,
                    strokeWidth = 1.2f,
                    cap = StrokeCap.Round
                )
            }
        }

        repeat(64) { index ->
            val xNoise = sin(index * 12.9898f + 0.41f).toFloat() * 0.5f + 0.5f
            val yNoise = sin(index * 78.233f + 1.73f).toFloat() * 0.5f + 0.5f
            val xDrift = sin(index * 3.17f).toFloat() * size.width * 0.025f
            val x = size.width * (0.06f + xNoise * 0.88f) + xDrift
            val y = size.height * (0.035f + yNoise * 0.42f)
            val twinkle = 0.55f + 0.45f * sin((drift * 2.0 * PI + index * 0.83f).toFloat())
            val radius = 1.0f + (index % 4) * 0.55f
            drawCircle(
                color = AppColors.Star.copy(alpha = (0.22f + twinkle * 0.34f).coerceIn(0f, 0.56f)),
                radius = radius,
                center = Offset(x, y)
            )
        }

        constellations.flatten().forEachIndexed { index, star ->
            val twinkle = 0.68f + 0.32f * sin((drift * 2.0 * PI + index * 1.4f).toFloat())
            drawCircle(
                color = AppColors.Star.copy(alpha = 0.52f + twinkle * 0.24f),
                radius = 2.3f,
                center = star
            )
        }

        if (shootingStar < 0.42f) {
            val progress = shootingStar / 0.42f
            val head = Offset(
                x = size.width * (0.12f + progress * 0.78f),
                y = size.height * (0.12f + progress * 0.14f)
            )
            val tail = Offset(head.x - size.width * 0.18f, head.y - size.height * 0.07f)
            val alpha = sin((progress * PI).toFloat()).coerceIn(0f, 1f) * 0.72f
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        AppColors.StarTrail.copy(alpha = alpha),
                        AppColors.Star.copy(alpha = alpha * 0.75f)
                    ),
                    start = tail,
                    end = head
                ),
                start = tail,
                end = head,
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            drawCircle(
                color = AppColors.Star.copy(alpha = alpha),
                radius = 3.0f,
                center = head
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    AppColors.EmberWash.copy(alpha = 0.12f * pulse),
                    AppColors.EmberGlow.copy(alpha = 0.22f * pulse)
                ),
                startY = size.height * 0.52f,
                endY = size.height
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    AppColors.EmberGlow.copy(alpha = 0.32f * pulse),
                    AppColors.EmberWash.copy(alpha = 0.16f * pulse),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.5f, size.height * 0.95f),
                radius = size.width * 0.56f
            ),
            radius = size.width * 0.56f,
            center = Offset(size.width * 0.5f, size.height * 0.95f)
        )

        repeat(26) { index ->
            val seed = index * 0.137f
            val rise = (drift + seed) % 1f
            val wave = sin((drift * 2.0 * PI + index).toFloat())
            val x = size.width * (0.12f + ((index * 0.061f) % 0.76f)) + wave * 14f
            val y = size.height * (0.93f - rise * 0.16f)
            val radius = 2.3f + (index % 4) * 1.5f
            val alpha = (1f - rise) * 0.28f * pulse
            drawCircle(
                color = AppColors.EmberSpark.copy(alpha = alpha.coerceIn(0f, 0.3f)),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun InfoDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextMuted,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "OK", color = AppColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(PAYPAL_DONATION_URL) }) {
                Text(text = "Donate with PayPal", color = AppColors.Accent)
            }
        },
        title = {
            Text(
                text = "About Free Noise Generator",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(
                text = "Created by Noisyogui.\n\nA calm tool for deep noise, focus, rest, and better sleep. Shape the low end, set a timer, and let the room soften.\n\nIf this app helps you relax, focus, or sleep a little better, you can support the project with a small PayPal donation."
            )
        }
    )
}

@Composable
private fun TimerSlider(
    steps: Int,
    timerEndsAtMillis: Long,
    nowMillis: Long,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Timer",
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = timerDisplayText(steps, timerEndsAtMillis, nowMillis),
                color = AppColors.TextMuted,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = steps.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(0, MAX_TIMER_STEPS)) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..MAX_TIMER_STEPS.toFloat(),
            steps = MAX_TIMER_STEPS - 1,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.TimerAccent,
                activeTrackColor = AppColors.TimerAccent,
                inactiveTrackColor = AppColors.Track,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun BandSlider(
    name: String,
    value: Float,
    maxValue: Float = MAX_BAND_LEVEL,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                color = AppColors.TextPrimary,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${((value / maxValue) * 100).toInt()}%",
                color = AppColors.TextMuted,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..maxValue,
            colors = SliderDefaults.colors(
                thumbColor = AppColors.BandAccent,
                activeTrackColor = AppColors.BandAccent,
                inactiveTrackColor = AppColors.Track,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private object AppColors {
    val Background = Color(0xFF000000)
    val Panel = Color(0xFF090909)
    val PanelBorder = Color(0xFF2A2A2A)
    val ButtonIdle = Color(0xFFF4F4F4)
    val ButtonIdleText = Color(0xFF050505)
    val ButtonActive = Color(0xFF191919)
    val ButtonBorder = Color(0xFF6F6F6F)
    val ButtonBorderActive = Color(0xFFE8E8E8)
    val Accent = Color(0xFFFFFFFF)
    val BandAccent = Color(0xFFE6E6E6)
    val TimerAccent = Color(0xFFCFCFCF)
    val Track = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFA8A8A8)
    val IdleOverlay = Color(0xEA000000)
    val NightSky = Color(0xFF10152A)
    val MilkyWayBlue = Color(0xFF4B8FEA)
    val MilkyWayViolet = Color(0xFF8E65D8)
    val Star = Color(0xFFEAF3FF)
    val StarTrail = Color(0xFFA8C7FF)
    val EmberWash = Color(0xFF4A1C0C)
    val EmberGlow = Color(0xFFC16424)
    val EmberSpark = Color(0xFFFFC07A)
}

private fun Context.startNoisePlayback(
    volume: Float,
    bassLevel: Float,
    lowMidsLevel: Float,
    timerMillis: Long
) {
    val intent = NoiseService.playIntent(
        context = this,
        volume = volume.coerceIn(0f, 1f),
        bassLevel = bassLevel.coerceIn(0f, MAX_BAND_LEVEL),
        lowMidsLevel = lowMidsLevel.coerceIn(0f, MAX_LOW_MIDS_LEVEL),
        timerMillis = timerMillis
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

private fun Context.stopNoisePlayback() {
    startService(NoiseService.stopIntent(this))
}

private fun Int.toTimerMillis(): Long =
    this.toLong() * TIMER_STEP_MILLIS

private fun timerDisplayText(steps: Int, timerEndsAtMillis: Long, nowMillis: Long): String {
    if (timerEndsAtMillis > 0L) {
        return ((timerEndsAtMillis - nowMillis).coerceAtLeast(0L) / 1000L).toCountdownLabel()
    }
    return steps.toDurationLabel()
}

private fun Int.toDurationLabel(): String {
    if (this == 0) return "Off"
    val totalMinutes = this * TIMER_STEP_MINUTES
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0 -> "${minutes}m"
        minutes == 0 -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

private fun Long.toCountdownLabel(): String {
    val hours = this / 3600L
    val minutes = (this % 3600L) / 60L
    val seconds = this % 60L
    return "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private const val KEY_VOLUME = "volume"
private const val KEY_BASS_LEVEL = "bass_level"
private const val KEY_LOW_MIDS_LEVEL = "low_mids_level"
private const val KEY_TIMER_STEPS = "timer_steps"
private const val KEY_SCREENSAVER_ENABLED = "screensaver_enabled"
private const val DEFAULT_VOLUME = 0.85f
private const val MAX_BAND_LEVEL = 0.5f
private const val MAX_LOW_MIDS_LEVEL = 0.1f
private const val DEFAULT_BASS_LEVEL = 0.5f
private const val DEFAULT_LOW_MIDS_LEVEL = 0.1f
private const val DEFAULT_TIMER_STEPS = 0
private const val MAX_TIMER_STEPS = 24
private const val TIMER_STEP_MINUTES = 30
private const val TIMER_STEP_MILLIS = TIMER_STEP_MINUTES * 60L * 1000L
private const val TIMER_APPLY_DELAY_MILLIS = 5_000L
private const val IDLE_DIM_DELAY_MILLIS = 10_000L
private const val PAYPAL_DONATION_URL =
    "https://www.paypal.com/donate/?business=VDWKX7KYKZB9Q&no_recurring=1&currency_code=EUR"
