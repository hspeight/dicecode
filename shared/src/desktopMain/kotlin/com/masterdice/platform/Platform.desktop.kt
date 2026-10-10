package com.masterdice.platform

import java.io.File

actual fun defaultStorageDir(): String =
    File(System.getProperty("user.home"), ".masterdice").absolutePath

// Desktop has no release variant; on by default, -Dmasterdice.debug=false to hide.
actual val debugToolsEnabled: Boolean
    get() = System.getProperty("masterdice.debug")?.toBooleanStrictOrNull() ?: true
