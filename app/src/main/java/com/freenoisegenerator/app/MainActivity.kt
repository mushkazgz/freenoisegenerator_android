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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.freenoisegenerator.app.service.NoiseService
import com.freenoisegenerator.app.ui.theme.FreeNoiseGeneratorTheme

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

    LaunchedEffect(lastInteractionMillis) {
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
                detectTapGestures(
                    onPress = {
                        registerInteraction()
                        tryAwaitRelease()
                    }
                )
            }
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
        if (isDimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.IdleOverlay)
            )
        }
    }
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
    val Background = Color(0xFF060909)
    val Panel = Color(0xFF101817)
    val PanelBorder = Color(0xFF1D2A28)
    val ButtonIdle = Color(0xFFBFEFD9)
    val ButtonIdleText = Color(0xFF08231C)
    val ButtonActive = Color(0xFF20302D)
    val ButtonBorder = Color(0xFF47665D)
    val ButtonBorderActive = Color(0xFF7FCFB6)
    val Accent = Color(0xFFD9B86F)
    val BandAccent = Color(0xFF8FC7FF)
    val TimerAccent = Color(0xFFD7A7FF)
    val Track = Color(0xFF283A36)
    val TextPrimary = Color(0xFFF1F7F4)
    val TextMuted = Color(0xFF9BAEA7)
    val IdleOverlay = Color(0xE6000000)
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
