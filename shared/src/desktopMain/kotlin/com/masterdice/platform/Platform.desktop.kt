package com.masterdice.platform

import java.io.File

actual fun defaultStorageDir(): String =
    File(System.getProperty("user.home"), ".masterdice").absolutePath
