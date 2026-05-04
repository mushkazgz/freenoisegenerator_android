package com.freenoisegenerator.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.freenoisegenerator.app.MainActivity
import com.freenoisegenerator.app.R
import com.freenoisegenerator.app.audio.NoiseAudioEngine
import com.freenoisegenerator.app.audio.NoiseKind

class NoiseService : Service() {
    private val engine = NoiseAudioEngine()
    private val handler = Handler(Looper.getMainLooper())
    private var currentKind = NoiseKind.WHITE
    private var timerRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play(intent)
            ACTION_PAUSE, ACTION_STOP -> stopPlayback()
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopTimer()
        engine.stop()
        broadcastPlaying(false)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun play(intent: Intent) {
        currentKind = NoiseKind.fromName(intent.getStringExtra(EXTRA_KIND))
        val volume = intent.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME)
        val tone = intent.getFloatExtra(EXTRA_TONE, DEFAULT_TONE)
        val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)

        startForeground(NOTIFICATION_ID, notification(isPlaying = true))
        engine.start(currentKind, volume, tone)
        engine.setKind(currentKind)
        engine.setVolume(volume)
        engine.setTone(tone)
        scheduleTimer(durationMillis)
        broadcastPlaying(true)
    }

    private fun stopPlayback() {
        stopTimer()
        engine.stop()
        broadcastPlaying(false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun scheduleTimer(durationMillis: Long) {
        stopTimer()
        if (durationMillis <= 0L) return

        timerRunnable = Runnable { stopPlayback() }.also {
            handler.postDelayed(it, durationMillis)
        }
    }

    private fun stopTimer() {
        timerRunnable?.let(handler::removeCallbacks)
        timerRunnable = null
    }

    private fun notification(isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val pauseIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, NoiseService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, NoiseService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_noise)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("${currentKind.label} en reproduccion")
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Pausar", pauseIntent)
            .addAction(0, "Parar", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reproduccion de ruido",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun broadcastPlaying(isPlaying: Boolean) {
        val intent = Intent(ACTION_STATE_CHANGED)
            .setPackage(packageName)
            .putExtra(EXTRA_IS_PLAYING, isPlaying)
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_PLAY = "com.freenoisegenerator.app.action.PLAY"
        const val ACTION_PAUSE = "com.freenoisegenerator.app.action.PAUSE"
        const val ACTION_STOP = "com.freenoisegenerator.app.action.STOP"
        const val ACTION_STATE_CHANGED = "com.freenoisegenerator.app.action.STATE_CHANGED"
        const val EXTRA_KIND = "extra_kind"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_TONE = "extra_tone"
        const val EXTRA_DURATION_MILLIS = "extra_duration_millis"
        const val EXTRA_IS_PLAYING = "extra_is_playing"

        private const val CHANNEL_ID = "noise_playback"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_VOLUME = 0.55f
        private const val DEFAULT_TONE = 0.18f

        fun playIntent(
            context: Context,
            kind: NoiseKind,
            volume: Float,
            durationMillis: Long,
            tone: Float = DEFAULT_TONE
        ): Intent =
            Intent(context, NoiseService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_KIND, kind.name)
                .putExtra(EXTRA_VOLUME, volume)
                .putExtra(EXTRA_TONE, tone)
                .putExtra(EXTRA_DURATION_MILLIS, durationMillis)

        fun stopIntent(context: Context): Intent =
            Intent(context, NoiseService::class.java).setAction(ACTION_STOP)
    }
}
