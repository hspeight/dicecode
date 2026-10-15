package com.masterdice.platform

import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

actual fun defaultStorageDir(): String =
    File(System.getProperty("user.home"), ".masterdice").absolutePath

// Desktop has no release variant; on by default, -Dmasterdice.debug=false to hide.
actual val debugToolsEnabled: Boolean
    get() = System.getProperty("masterdice.debug")?.toBooleanStrictOrNull() ?: true

private const val RATE = 44100f
private val tapPcm: ByteArray by lazy {
    val n = (RATE * 0.045f).toInt()
    ByteArray(n * 2) { i ->
        val t = (i / 2) / RATE
        val v = (sin(2.0 * PI * 2200 * t) * exp(-t * 110.0) *
            0.4 * Short.MAX_VALUE).toInt()
        if (i % 2 == 0) (v and 0xff).toByte() else (v shr 8).toByte()
    }
}

// One reusable clip; rewound and replayed per tap.
private var tapClip: Clip? = null

actual fun playTapSound() {
    val clip = tapClip ?: AudioSystem.getClip().also {
        it.open(AudioFormat(RATE, 16, 1, true, false), tapPcm, 0, tapPcm.size)
        tapClip = it
    }
    clip.stop()
    clip.framePosition = 0
    clip.start()
}

// Rising two-tone chirp for submit — 880Hz for 70ms, then 1320Hz decaying.
private val submitPcm: ByteArray by lazy {
    val n = (RATE * 0.16f).toInt()
    ByteArray(n * 2) { i ->
        val t = (i / 2) / RATE
        val f = if (t < 0.07f) 880.0 else 1320.0
        val td = if (t < 0.07f) 0.0 else t - 0.07
        val v = (sin(2.0 * PI * f * t) * exp(-td * 35.0) *
            0.4 * Short.MAX_VALUE).toInt()
        if (i % 2 == 0) (v and 0xff).toByte() else (v shr 8).toByte()
    }
}

private var submitClip: Clip? = null

actual fun playSubmitSound() {
    val clip = submitClip ?: AudioSystem.getClip().also {
        it.open(
            AudioFormat(RATE, 16, 1, true, false),
            submitPcm, 0, submitPcm.size,
        )
        submitClip = it
    }
    clip.stop()
    clip.framePosition = 0
    clip.start()
}
