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

// One reusable static-mode track; rewound and replayed per tap.
private var tapTrack: AudioTrack? = null

actual fun playTapSound() {
    val track = tapTrack ?: AudioTrack.Builder()
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
        .setBufferSizeInBytes(tapPcm.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build().also {
            it.write(tapPcm, 0, tapPcm.size)
            tapTrack = it
        }
    track.stop()
    track.playbackHeadPosition = 0
    track.play()
}
