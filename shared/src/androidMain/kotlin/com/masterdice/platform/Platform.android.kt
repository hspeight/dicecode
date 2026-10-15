package com.masterdice.platform

import android.content.Context
import android.content.pm.ApplicationInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

private lateinit var appContext: Context

/** Must be called once from the Android Application or Activity. */
fun installAndroidContext(context: Context) {
    appContext = context.applicationContext
}

actual fun defaultStorageDir(): String = appContext.filesDir.absolutePath

actual val debugToolsEnabled: Boolean
    get() = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

private const val RATE = 22050
private val tapPcm: ShortArray by lazy {
    val n = (RATE * 0.045f).toInt()
    ShortArray(n) { i ->
        val t = i / RATE.toFloat()
        (sin(2.0 * PI * 2200 * t) * exp(-t * 110.0) *
            0.4 * Short.MAX_VALUE).toInt().toShort()
    }
}

// Reusable static-mode tracks; rewound and replayed per tap/submit.
private fun newStaticTrack(pcm: ShortArray): AudioTrack = AudioTrack.Builder()
    .setAudioAttributes(
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
    )
    .setAudioFormat(
        AudioFormat.Builder()
            .setSampleRate(RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build(),
    )
    .setBufferSizeInBytes(pcm.size * 2)
    .setTransferMode(AudioTrack.MODE_STATIC)
    .build().also { it.write(pcm, 0, pcm.size) }

private var tapTrack: AudioTrack? = null

actual fun playTapSound() {
    val track = tapTrack ?: newStaticTrack(tapPcm).also { tapTrack = it }
    track.stop()
    track.playbackHeadPosition = 0
    track.play()
}

// Rising two-tone chirp for submit — 880Hz for 70ms, then 1320Hz decaying.
private val submitPcm: ShortArray by lazy {
    val n = (RATE * 0.16f).toInt()
    ShortArray(n) { i ->
        val t = i / RATE.toFloat()
        val f = if (t < 0.07f) 880.0 else 1320.0
        val td = if (t < 0.07f) 0.0f else t - 0.07f
        (sin(2.0 * PI * f * t) * exp(-td * 35.0) *
            0.4 * Short.MAX_VALUE).toInt().toShort()
    }
}

private var submitTrack: AudioTrack? = null

actual fun playSubmitSound() {
    val track = submitTrack ?: newStaticTrack(submitPcm).also { submitTrack = it }
    track.stop()
    track.playbackHeadPosition = 0
    track.play()
}
