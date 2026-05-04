package com.freenoisegenerator.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class NoiseAudioEngine {
    @Volatile
    private var running = false

    @Volatile
    private var targetVolume = 0.55f

    @Volatile
    private var bassLevel = DEFAULT_BASS_LEVEL

    @Volatile
    private var lowMidsLevel = DEFAULT_LOW_MIDS_LEVEL

    private var audioThread: Thread? = null

    fun start(
        initialVolume: Float,
        initialBassLevel: Float = DEFAULT_BASS_LEVEL,
        initialLowMidsLevel: Float = DEFAULT_LOW_MIDS_LEVEL
    ) {
        targetVolume = initialVolume.coerceIn(0f, 1f)
        bassLevel = initialBassLevel.coerceIn(0f, MAX_BAND_LEVEL)
        lowMidsLevel = initialLowMidsLevel.coerceIn(0f, MAX_LOW_MIDS_LEVEL)
        if (running) return

        running = true
        audioThread = Thread(::audioLoop, "NoiseAudioEngine").also { it.start() }
    }

    fun setVolume(nextVolume: Float) {
        targetVolume = nextVolume.coerceIn(0f, 1f)
    }

    fun setBandLevels(nextBassLevel: Float, nextLowMidsLevel: Float) {
        bassLevel = nextBassLevel.coerceIn(0f, MAX_BAND_LEVEL)
        lowMidsLevel = nextLowMidsLevel.coerceIn(0f, MAX_LOW_MIDS_LEVEL)
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
                    val sample = generator.next(bassLevel, lowMidsLevel) * currentVolume * OUTPUT_GAIN
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
        private val brownProfile = BrownBandProfile()

        fun next(bassLevel: Float, lowMidsLevel: Float): Float {
            val white = random.nextFloat() * 2.0 - 1.0
            return brownProfile.next(white, bassLevel, lowMidsLevel).toFloat()
        }

        private class BrownBandProfile {
            private val bass = FilterBand(
                minFrequency = 32.0,
                centerFrequency = 125.0,
                maxFrequency = 500.0,
                q = 0.7
            )
            private val lowMids = FilterBand(
                minFrequency = 500.0,
                centerFrequency = 1000.0,
                maxFrequency = 2000.0,
                q = 0.7
            )

            fun next(input: Double, bassLevel: Float, lowMidsLevel: Float): Double {
                val bassGain = bassLevel.toDouble() / MAX_BAND_LEVEL
                val lowMidsGain = lowMidsLevel.toDouble() / MAX_BAND_LEVEL
                return (bass.next(input) * bassGain + lowMids.next(input) * lowMidsGain) *
                    BROWN_PROFILE_GAIN
            }
        }

        private class FilterBand(
            minFrequency: Double,
            centerFrequency: Double,
            maxFrequency: Double,
            q: Double
        ) {
            private val lowCut = Biquad.highPass(minFrequency, 1.0)
            private val bandPass = Biquad.bandPass(centerFrequency, q)
            private val highCut = Biquad.lowPass(maxFrequency, 1.0)

            fun next(input: Double): Double =
                highCut.process(bandPass.process(lowCut.process(input)))
        }

        private class Biquad(
            private val b0: Double,
            private val b1: Double,
            private val b2: Double,
            private val a1: Double,
            private val a2: Double
        ) {
            private var x1 = 0.0
            private var x2 = 0.0
            private var y1 = 0.0
            private var y2 = 0.0

            fun process(input: Double): Double {
                val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
                x2 = x1
                x1 = input
                y2 = y1
                y1 = output
                return output
            }

            companion object {
                fun lowPass(frequency: Double, q: Double): Biquad {
                    val omega = 2.0 * PI * frequency / SAMPLE_RATE
                    val alpha = sin(omega) / (2.0 * q)
                    val cosOmega = cos(omega)
                    val b0 = (1.0 - cosOmega) / 2.0
                    val b1 = 1.0 - cosOmega
                    val b2 = (1.0 - cosOmega) / 2.0
                    val a0 = 1.0 + alpha
                    val a1 = -2.0 * cosOmega
                    val a2 = 1.0 - alpha
                    return normalized(b0, b1, b2, a0, a1, a2)
                }

                fun highPass(frequency: Double, q: Double): Biquad {
                    val omega = 2.0 * PI * frequency / SAMPLE_RATE
                    val alpha = sin(omega) / (2.0 * q)
                    val cosOmega = cos(omega)
                    val b0 = (1.0 + cosOmega) / 2.0
                    val b1 = -(1.0 + cosOmega)
                    val b2 = (1.0 + cosOmega) / 2.0
                    val a0 = 1.0 + alpha
                    val a1 = -2.0 * cosOmega
                    val a2 = 1.0 - alpha
                    return normalized(b0, b1, b2, a0, a1, a2)
                }

                fun bandPass(frequency: Double, q: Double): Biquad {
                    val omega = 2.0 * PI * frequency / SAMPLE_RATE
                    val alpha = sin(omega) / (2.0 * q)
                    val cosOmega = cos(omega)
                    val b0 = alpha
                    val b1 = 0.0
                    val b2 = -alpha
                    val a0 = 1.0 + alpha
                    val a1 = -2.0 * cosOmega
                    val a2 = 1.0 - alpha
                    return normalized(b0, b1, b2, a0, a1, a2)
                }

                private fun normalized(
                    b0: Double,
                    b1: Double,
                    b2: Double,
                    a0: Double,
                    a1: Double,
                    a2: Double
                ): Biquad =
                    Biquad(
                        b0 = b0 / a0,
                        b1 = b1 / a0,
                        b2 = b2 / a0,
                        a1 = a1 / a0,
                        a2 = a2 / a0
                    )
            }
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val VOLUME_SMOOTHING = 0.0007f
        const val OUTPUT_GAIN = 1.5f
        const val BROWN_PROFILE_GAIN = 2.8
        const val MAX_BAND_LEVEL = 0.5f
        const val MAX_LOW_MIDS_LEVEL = 0.1f
        const val DEFAULT_BASS_LEVEL = 0.5f
        const val DEFAULT_LOW_MIDS_LEVEL = 0.1f
    }
}
