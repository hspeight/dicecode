package com.masterdice.platform

/** Directory where the in-progress game is persisted. */
expect fun defaultStorageDir(): String

/** True only in debug builds — gates the fixed-deal dev tools. */
expect val debugToolsEnabled: Boolean

/** Play a short tap sound. Must never throw — audio failures are silent. */
expect fun playTapSound()

/** Sound-effect gate: the settings toggle flips [enabled]; taps call [tap]. */
object SoundFx {
    var enabled = true
    fun tap() {
        if (enabled) {
            try {
                playTapSound()
            } catch (_: Throwable) {
            }
        }
    }
}
