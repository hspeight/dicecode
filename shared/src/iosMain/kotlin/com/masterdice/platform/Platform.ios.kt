package com.masterdice.platform

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
