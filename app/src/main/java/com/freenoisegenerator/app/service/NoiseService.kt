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

class NoiseService : Service() {
    private val engine = NoiseAudioEngine()
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null
    private var currentVolume = DEFAULT_VOLUME
    private var currentBassLevel = DEFAULT_BASS_LEVEL
    private var currentLowMidsLevel = DEFAULT_LOW_MIDS_LEVEL

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play(intent)
            ACTION_PAUSE -> pausePlayback()
            ACTION_STOP -> stopPlayback()
            ACTION_STOP_FROM_NOTIFICATION -> stopPlayback(closeApp = true)
            else -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        cancelTimer()
        engine.stop()
        broadcastState(isPlaying = false, timerEndsAtMillis = 0L)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!playbackSnapshot().isPlaying) {
            stopPlayback()
        }
        super.onTaskRemoved(rootIntent)
    }

    private fun play(intent: Intent) {
        val volume = intent.getFloatExtra(EXTRA_VOLUME, DEFAULT_VOLUME)
        val bassLevel = intent.getFloatExtra(EXTRA_BASS_LEVEL, DEFAULT_BASS_LEVEL)
        val lowMidsLevel = intent.getFloatExtra(EXTRA_LOW_MIDS_LEVEL, DEFAULT_LOW_MIDS_LEVEL)
        val timerMillis = intent.getLongExtra(EXTRA_TIMER_MILLIS, 0L)
        val timerEndsAtMillis = if (timerMillis > 0L) {
            System.currentTimeMillis() + timerMillis
        } else {
            0L
        }

        currentVolume = volume
        currentBassLevel = bassLevel
        currentLowMidsLevel = lowMidsLevel
        if (volume <= 0f) {
            pausePlayback()
            return
        }
        startForeground(NOTIFICATION_ID, notification(isPlaying = true))
        engine.start(volume, bassLevel, lowMidsLevel)
        engine.setVolume(volume)
        engine.setBandLevels(bassLevel, lowMidsLevel)
        scheduleTimer(timerMillis)
        broadcastState(isPlaying = true, timerEndsAtMillis = timerEndsAtMillis)
    }

    private fun pausePlayback() {
        cancelTimer()
        engine.stop()
        broadcastState(isPlaying = false, timerEndsAtMillis = 0L)
        startForeground(NOTIFICATION_ID, notification(isPlaying = false))
    }

    private fun stopPlayback(
        closeApp: Boolean = false,
        timerFinished: Boolean = false
    ) {
        cancelTimer()
        engine.stop()
        broadcastState(
            isPlaying = false,
            timerEndsAtMillis = 0L,
            timerFinished = timerFinished
        )
        if (closeApp) {
            broadcastCloseApp()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun scheduleTimer(timerMillis: Long) {
        cancelTimer()
        if (timerMillis <= 0L) return

        stopRunnable = Runnable { stopPlayback(timerFinished = true) }.also {
            handler.postDelayed(it, timerMillis)
        }
    }

    private fun cancelTimer() {
        stopRunnable?.let(handler::removeCallbacks)
        stopRunnable = null
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
        val playPendingIntent = PendingIntent.getService(
            this,
            3,
            playIntent(
                context = this,
                volume = currentVolume,
                bassLevel = currentBassLevel,
                lowMidsLevel = currentLowMidsLevel,
                timerMillis = 0L
            ),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, NoiseService::class.java).setAction(ACTION_STOP_FROM_NOTIFICATION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playbackAction = if (isPlaying) {
            NotificationCompat.Action(R.drawable.ic_notification_pause, "Pause", pauseIntent)
        } else {
            NotificationCompat.Action(R.drawable.ic_notification_play, "Play", playPendingIntent)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_noise)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(if (isPlaying) "Brown noise playing" else "Brown noise paused")
            .setContentIntent(contentIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1)
            )
            .addAction(playbackAction)
            .addAction(R.drawable.ic_notification_stop, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Noise playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun broadcastState(
        isPlaying: Boolean,
        timerEndsAtMillis: Long,
        timerFinished: Boolean = false
    ) {
        playbackSnapshot = PlaybackSnapshot(
            isPlaying = isPlaying,
            volume = currentVolume,
            timerEndsAtMillis = timerEndsAtMillis
        )
        val intent = Intent(ACTION_STATE_CHANGED)
            .setPackage(packageName)
            .putExtra(EXTRA_IS_PLAYING, isPlaying)
            .putExtra(EXTRA_TIMER_ENDS_AT_MILLIS, timerEndsAtMillis)
            .putExtra(EXTRA_TIMER_FINISHED, timerFinished)
        sendBroadcast(intent)
    }

    private fun broadcastCloseApp() {
        val intent = Intent(ACTION_CLOSE_APP).setPackage(packageName)
        sendBroadcast(intent)
    }

    companion object {
        data class PlaybackSnapshot(
            val isPlaying: Boolean,
            val volume: Float,
            val timerEndsAtMillis: Long
        )

        @Volatile
        private var playbackSnapshot = PlaybackSnapshot(
            isPlaying = false,
            volume = 0f,
            timerEndsAtMillis = 0L
        )

        const val ACTION_PLAY = "com.freenoisegenerator.app.action.PLAY"
        const val ACTION_PAUSE = "com.freenoisegenerator.app.action.PAUSE"
        const val ACTION_STOP = "com.freenoisegenerator.app.action.STOP"
        const val ACTION_STOP_FROM_NOTIFICATION = "com.freenoisegenerator.app.action.STOP_FROM_NOTIFICATION"
        const val ACTION_CLOSE_APP = "com.freenoisegenerator.app.action.CLOSE_APP"
        const val ACTION_STATE_CHANGED = "com.freenoisegenerator.app.action.STATE_CHANGED"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_BASS_LEVEL = "extra_bass_level"
        const val EXTRA_LOW_MIDS_LEVEL = "extra_low_mids_level"
        const val EXTRA_TIMER_MILLIS = "extra_timer_millis"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_TIMER_ENDS_AT_MILLIS = "extra_timer_ends_at_millis"
        const val EXTRA_TIMER_FINISHED = "extra_timer_finished"

        private const val CHANNEL_ID = "noise_playback"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_VOLUME = 0.55f
        private const val DEFAULT_BASS_LEVEL = 0.5f
        private const val DEFAULT_LOW_MIDS_LEVEL = 0.1f
        private const val MAX_LOW_MIDS_LEVEL = 0.1f

        fun playbackSnapshot(): PlaybackSnapshot = playbackSnapshot

        fun playIntent(
            context: Context,
            volume: Float,
            bassLevel: Float = DEFAULT_BASS_LEVEL,
            lowMidsLevel: Float = DEFAULT_LOW_MIDS_LEVEL,
            timerMillis: Long = 0L
        ): Intent =
            Intent(context, NoiseService::class.java)
                .setAction(ACTION_PLAY)
                .putExtra(EXTRA_VOLUME, volume)
                .putExtra(EXTRA_BASS_LEVEL, bassLevel)
                .putExtra(EXTRA_LOW_MIDS_LEVEL, lowMidsLevel.coerceIn(0f, MAX_LOW_MIDS_LEVEL))
                .putExtra(EXTRA_TIMER_MILLIS, timerMillis)

        fun stopIntent(context: Context): Intent =
            Intent(context, NoiseService::class.java).setAction(ACTION_STOP)
    }
}
