package com.masterdice.platform

/** Directory where the in-progress game is persisted. */
expect fun defaultStorageDir(): String

/** True in debug builds (or explicitly opted-in desktop development launches). */
expect val debugToolsEnabled: Boolean

/** Play a short tap sound. Must never throw — audio failures are silent. */
expect fun playTapSound()

/** Play the submit/confirm sound. Must never throw — failures are silent. */
expect fun playSubmitSound()

/** Sound-effect gate: the settings toggle flips [enabled]. */
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
    fun submit() {
        if (enabled) {
            try {
                playSubmitSound()
            } catch (_: Throwable) {
            }
        }
    }
}
