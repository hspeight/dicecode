package com.masterdice.platform

import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual fun defaultStorageDir(): String {
    val urls = NSFileManager.defaultManager.URLsForDirectory(
        NSDocumentDirectory, NSUserDomainMask
    )
    return (urls.first() as NSURL).path!!
}

@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
actual val debugToolsEnabled: Boolean get() = kotlin.native.Platform.isDebugBinary

// iOS system sound 1104 ("Tock") — a light tap, no bundled asset needed.
actual fun playTapSound() {
    AudioServicesPlaySystemSound(1104u)
}
