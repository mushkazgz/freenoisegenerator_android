package com.freenoisegenerator.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.freenoisegenerator.app.audio.NoiseKind
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
    var tone by remember {
        mutableStateOf(preferences.getFloat(KEY_TONE, DEFAULT_TONE))
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == NoiseService.ACTION_STATE_CHANGED) {
                    isPlaying = intent.getBooleanExtra(NoiseService.EXTRA_IS_PLAYING, false)
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
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    fun saveVolume() {
        preferences.edit().putFloat(KEY_VOLUME, volume).apply()
    }

    fun saveTone() {
        preferences.edit().putFloat(KEY_TONE, tone).apply()
    }

    fun play() {
        saveVolume()
        saveTone()
        context.startNoisePlayback(volume, tone)
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
                                if (isPlaying) context.startNoisePlayback(volume, tone)
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
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tone",
                            color = AppColors.TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (tone < 0.38f) "Deep" else "Clear",
                            color = AppColors.TextMuted,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Slider(
                        value = tone,
                        onValueChange = { tone = it },
                        onValueChangeFinished = {
                            saveTone()
                            if (isPlaying) context.startNoisePlayback(volume, tone)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = AppColors.ToneAccent,
                            activeTrackColor = AppColors.ToneAccent,
                            inactiveTrackColor = AppColors.Track,
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deep brown",
                            color = AppColors.TextSubtle,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Lighter",
                            color = AppColors.TextSubtle,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
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
    val ToneAccent = Color(0xFF8AC7FF)
    val Track = Color(0xFF283A36)
    val TextPrimary = Color(0xFFF1F7F4)
    val TextMuted = Color(0xFF9BAEA7)
    val TextSubtle = Color(0xFF6F817C)
}

private fun Context.startNoisePlayback(volume: Float, tone: Float) {
    val intent = NoiseService.playIntent(
        context = this,
        kind = NoiseKind.BROWN,
        volume = volume.coerceIn(0f, 1f),
        durationMillis = 0L,
        tone = tone.coerceIn(0f, 1f)
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

private const val KEY_VOLUME = "volume"
private const val KEY_TONE = "tone"
private const val DEFAULT_VOLUME = 0.85f
private const val DEFAULT_TONE = 0.18f
