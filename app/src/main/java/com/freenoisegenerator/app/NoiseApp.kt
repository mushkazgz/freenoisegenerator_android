package com.freenoisegenerator.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.freenoisegenerator.app.service.NoiseService
import com.freenoisegenerator.app.ui.mainframe.MainFrameScreen
import com.freenoisegenerator.app.ui.screensaver.PixelFireScreensaver

@Composable
internal fun NoiseApp(resumeSignal: Int) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("noise_settings", Context.MODE_PRIVATE)
    }
    var isPlaying by remember { mutableStateOf(false) }
    var volume by remember {
        mutableStateOf(preferences.getFloat(KEY_VOLUME, DEFAULT_VOLUME))
    }
    var bassLevel by remember {
        mutableStateOf(
            preferences.getFloat(KEY_BASS_LEVEL, DEFAULT_BASS_LEVEL)
                .coerceIn(0f, MAX_BAND_LEVEL)
        )
    }
    var lowMidsLevel by remember {
        mutableStateOf(
            preferences.getFloat(KEY_LOW_MIDS_LEVEL, DEFAULT_LOW_MIDS_LEVEL)
                .coerceIn(0f, MAX_LOW_MIDS_LEVEL)
        )
    }
    var timerSteps by remember {
        mutableStateOf(
            preferences.getInt(KEY_TIMER_STEPS, DEFAULT_TIMER_STEPS)
                .coerceIn(0, MAX_TIMER_STEPS)
        )
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

    ScreensaverSystemBars(hidden = isDimmed)

    fun registerInteraction() {
        lastInteractionMillis = System.currentTimeMillis()
        isDimmed = false
    }

    LaunchedEffect(resumeSignal) {
        registerInteraction()
    }

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                when (intent?.action) {
                    NoiseService.ACTION_STATE_CHANGED -> {
                        isPlaying = intent.getBooleanExtra(NoiseService.EXTRA_IS_PLAYING, false)
                        timerEndsAtMillis = intent.getLongExtra(
                            NoiseService.EXTRA_TIMER_ENDS_AT_MILLIS,
                            0L
                        )
                    }

                    NoiseService.ACTION_CLOSE_APP -> activity?.finishAndRemoveTask()
                }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter().apply {
                addAction(NoiseService.ACTION_STATE_CHANGED)
                addAction(NoiseService.ACTION_CLOSE_APP)
            },
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
        preferences.edit()
            .putInt(KEY_TIMER_STEPS, timerSteps.coerceIn(0, MAX_TIMER_STEPS))
            .apply()
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

    fun updatePlayback(
        nextVolume: Float = volume,
        nextBassLevel: Float = bassLevel,
        nextLowMidsLevel: Float = lowMidsLevel
    ) {
        if (!isPlaying) return
        context.startNoisePlayback(
            volume = nextVolume,
            bassLevel = nextBassLevel,
            lowMidsLevel = nextLowMidsLevel,
            timerMillis = timerSteps.toTimerMillis()
        )
    }

    fun play() {
        saveVolume()
        saveBandLevels()
        saveTimer()
        startPlayback()
        isPlaying = true
    }

    fun pause() {
        context.pauseNoisePlayback()
        isPlaying = false
    }

    fun selectTimer(nextSteps: Int) {
        timerSteps = nextSteps.coerceIn(0, MAX_TIMER_STEPS)
        timerEndsAtMillis = 0L
        pendingTimerRunnable?.let(timerHandler::removeCallbacks)
        pendingTimerRunnable = null
    }

    fun applyTimerSelection() {
        saveTimer()
        pendingTimerRunnable?.let(timerHandler::removeCallbacks)
        val runnable = Runnable {
            if (isPlaying) startPlayback()
            nowMillis = System.currentTimeMillis()
            pendingTimerRunnable = null
        }
        pendingTimerRunnable = runnable
        timerHandler.postDelayed(runnable, TIMER_APPLY_DELAY_MILLIS)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        registerInteraction()
                    }
                }
            }
    ) {
        MainFrameScreen(
            isPlaying = isPlaying,
            volume = volume,
            bassLevel = bassLevel,
            maxBassLevel = MAX_BAND_LEVEL,
            lowMidsLevel = lowMidsLevel,
            maxLowMidsLevel = MAX_LOW_MIDS_LEVEL,
            timerSteps = timerSteps,
            maxTimerSteps = MAX_TIMER_STEPS,
            timerDescription = timerDisplayText(timerSteps, timerEndsAtMillis, nowMillis),
            onTogglePlayback = { if (isPlaying) pause() else play() },
            onVolumeChange = {
                volume = it
                updatePlayback(nextVolume = it)
            },
            onVolumeChangeFinished = ::saveVolume,
            onBassChange = {
                bassLevel = it
                updatePlayback(nextBassLevel = it)
            },
            onBassChangeFinished = ::saveBandLevels,
            onLowMidsChange = {
                lowMidsLevel = it
                updatePlayback(nextLowMidsLevel = it)
            },
            onLowMidsChangeFinished = ::saveBandLevels,
            onTimerChange = ::selectTimer,
            onTimerChangeFinished = ::applyTimerSelection,
            onSettingsClick = {
                registerInteraction()
                showSettingsDialog = true
            },
            modifier = Modifier.fillMaxSize()
        )

        AnimatedVisibility(
            visible = isDimmed,
            enter = fadeIn(animationSpec = tween(durationMillis = 1_600)),
            exit = fadeOut(animationSpec = tween(durationMillis = 450))
        ) {
            PixelFireScreensaver()
        }

        if (showInfoDialog) {
            InfoDialog(onDismiss = { showInfoDialog = false })
        }
        if (showSettingsDialog) {
            SettingsDialog(
                screensaverEnabled = screensaverEnabled,
                onScreensaverChange = ::setScreensaverEnabled,
                onBluetoothClick = {
                    registerInteraction()
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                },
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
private fun ScreensaverSystemBars(hidden: Boolean) {
    val view = LocalView.current

    DisposableEffect(view, hidden) {
        val activity = view.context as? ComponentActivity
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (hidden) {
            controller?.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            if (hidden) controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@Composable
private fun SettingsDialog(
    screensaverEnabled: Boolean,
    onScreensaverChange: (Boolean) -> Unit,
    onBluetoothClick: () -> Unit,
    onInfoClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppColors.Panel,
        titleContentColor = AppColors.TextPrimary,
        textContentColor = AppColors.TextMuted,
        confirmButton = { DialogActionButton(text = "Done", onClick = onDismiss) },
        dismissButton = { DialogActionButton(text = "Info", onClick = onInfoClick) },
        title = { Text(text = "Settings", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(text = "Screensaver")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsChoiceButton(
                        text = "On",
                        selected = screensaverEnabled,
                        onClick = { onScreensaverChange(true) }
                    )
                    SettingsChoiceButton(
                        text = "Off",
                        selected = !screensaverEnabled,
                        onClick = { onScreensaverChange(false) }
                    )
                }
                ElevatedButton(
                    onClick = onBluetoothClick,
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = AppColors.ButtonActive,
                        contentColor = AppColors.TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = "Bluetooth")
                }
            }
        }
    )
}

@Composable
private fun SettingsChoiceButton(text: String, selected: Boolean, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = if (selected) AppColors.ButtonIdle else AppColors.ButtonActive,
            contentColor = if (selected) AppColors.ButtonIdleText else AppColors.TextPrimary
        )
    ) {
        Text(text = text)
    }
}

@Composable
private fun DialogActionButton(text: String, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = AppColors.ButtonActive,
            contentColor = AppColors.TextPrimary
        )
    ) {
        Text(text = text)
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
            Text(text = "About Free Noise Generator", fontWeight = FontWeight.SemiBold)
        },
        text = {
            Text(
                text = "Created by Noisyogui.\n\nA calm tool for deep noise, focus, rest, " +
                    "and better sleep. Shape the low end, set a timer, and let the room " +
                    "soften.\n\nIf this app helps you relax, focus, or sleep a little better, " +
                    "you can support the project with a small PayPal donation."
            )
        }
    )
}

private object AppColors {
    val Panel = Color(0xFF090909)
    val ButtonIdle = Color(0xFFF4F4F4)
    val ButtonIdleText = Color(0xFF050505)
    val ButtonActive = Color(0xFF3C3C3C)
    val Accent = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFFB8B8B8)
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

private fun Context.pauseNoisePlayback() {
    startService(Intent(this, NoiseService::class.java).setAction(NoiseService.ACTION_PAUSE))
}

private fun Int.toTimerMillis(): Long = this.toLong() * TIMER_STEP_MILLIS

private fun timerDisplayText(steps: Int, timerEndsAtMillis: Long, nowMillis: Long): String {
    if (timerEndsAtMillis > 0L) {
        return ((timerEndsAtMillis - nowMillis).coerceAtLeast(0L) / 1_000L)
            .toCountdownLabel()
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
    val hours = this / 3_600L
    val minutes = (this % 3_600L) / 60L
    val seconds = this % 60L
    return "$hours:${minutes.toString().padStart(2, '0')}:" +
        seconds.toString().padStart(2, '0')
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
private const val TIMER_STEP_MILLIS = TIMER_STEP_MINUTES * 60L * 1_000L
private const val TIMER_APPLY_DELAY_MILLIS = 5_000L
private const val IDLE_DIM_DELAY_MILLIS = 10_000L
private const val PAYPAL_DONATION_URL =
    "https://www.paypal.com/donate/?business=VDWKX7KYKZB9Q&no_recurring=1&currency_code=EUR"
