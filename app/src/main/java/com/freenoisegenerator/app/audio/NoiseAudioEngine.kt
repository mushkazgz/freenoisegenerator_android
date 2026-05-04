package com.freenoisegenerator.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class NoiseAudioEngine {
    @Volatile
    private var running = false

    @Volatile
    private var kind = NoiseKind.WHITE

    @Volatile
    private var targetVolume = 0.55f

    @Volatile
    private var tone = DEFAULT_TONE

    private var audioThread: Thread? = null

    fun start(initialKind: NoiseKind, initialVolume: Float, initialTone: Float = DEFAULT_TONE) {
        kind = initialKind
        targetVolume = initialVolume.coerceIn(0f, 1f)
        tone = initialTone.coerceIn(0f, 1f)
        if (running) return

        running = true
        audioThread = Thread(::audioLoop, "NoiseAudioEngine").also { it.start() }
    }

    fun setKind(nextKind: NoiseKind) {
        kind = nextKind
    }

    fun setVolume(nextVolume: Float) {
        targetVolume = nextVolume.coerceIn(0f, 1f)
    }

    fun setTone(nextTone: Float) {
        tone = nextTone.coerceIn(0f, 1f)
    }

    fun stop() {
        running = false
        audioThread?.interrupt()
        audioThread = null
    }

    private fun audioLoop() {
        val minBufferBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSizeBytes = max(minBufferBytes, SAMPLE_RATE / 5)
        val frameCount = bufferSizeBytes / 2
        val buffer = ShortArray(frameCount)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        val generator = NoiseGenerator()
        var currentVolume = 0f

        try {
            track.play()
            while (running && !Thread.currentThread().isInterrupted) {
                for (index in buffer.indices) {
                    currentVolume += (targetVolume - currentVolume) * VOLUME_SMOOTHING
                    val sample = generator.next(kind, tone) * currentVolume * OUTPUT_GAIN
                    buffer[index] = (sample.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
                }
                track.write(buffer, 0, buffer.size)
            }
        } finally {
            runCatching {
                track.pause()
                track.flush()
                track.release()
            }
        }
    }

    private class NoiseGenerator {
        private val random = Random(System.nanoTime())
        private var pinkB0 = 0.0
        private var pinkB1 = 0.0
        private var pinkB2 = 0.0
        private var pinkB3 = 0.0
        private var pinkB4 = 0.0
        private var pinkB5 = 0.0
        private var pinkB6 = 0.0
        private var deepBrown = 0.0
        private var clearBrown = 0.0
        private var brownPower = TARGET_BROWN_RMS * TARGET_BROWN_RMS

        fun next(kind: NoiseKind, tone: Float): Float =
            when (kind) {
                NoiseKind.WHITE -> white()
                NoiseKind.PINK -> pink()
                NoiseKind.BROWN -> brown(tone)
            }

        private fun white(): Float =
            random.nextFloat() * 2f - 1f

        private fun pink(): Float {
            val white = white().toDouble()
            pinkB0 = 0.99886 * pinkB0 + white * 0.0555179
            pinkB1 = 0.99332 * pinkB1 + white * 0.0750759
            pinkB2 = 0.96900 * pinkB2 + white * 0.1538520
            pinkB3 = 0.86650 * pinkB3 + white * 0.3104856
            pinkB4 = 0.55000 * pinkB4 + white * 0.5329522
            pinkB5 = -0.7616 * pinkB5 - white * 0.0168980
            val output = pinkB0 + pinkB1 + pinkB2 + pinkB3 + pinkB4 + pinkB5 + pinkB6 + white * 0.5362
            pinkB6 = white * 0.115926
            return (output * 0.11).toFloat()
        }

        private fun brown(tone: Float): Float {
            val white = white().toDouble()
            deepBrown = (deepBrown + DEEP_BROWN_STEP * white) / (1.0 + DEEP_BROWN_STEP)
            clearBrown = (clearBrown + CLEAR_BROWN_STEP * white) / (1.0 + CLEAR_BROWN_STEP)
            deepBrown = min(1.0, max(-1.0, deepBrown))
            clearBrown = min(1.0, max(-1.0, clearBrown))

            val shaped = deepBrown * (1.0 - tone) + clearBrown * tone
            brownPower += (shaped * shaped - brownPower) * BROWN_POWER_SMOOTHING

            val gain = TARGET_BROWN_RMS / sqrt(brownPower + 0.000001)
            return (shaped * gain).coerceIn(-1.0, 1.0).toFloat()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val VOLUME_SMOOTHING = 0.0007f
        const val OUTPUT_GAIN = 1.5f
        const val DEFAULT_TONE = 0.18f
        const val DEEP_BROWN_STEP = 0.006
        const val CLEAR_BROWN_STEP = 0.055
        const val TARGET_BROWN_RMS = 0.24
        const val BROWN_POWER_SMOOTHING = 0.0008
    }
}
