package com.masterdice.platform

import android.content.Context
import android.content.pm.ApplicationInfo

private lateinit var appContext: Context

/** Must be called once from the Android Application or Activity. */
fun installAndroidContext(context: Context) {
    appContext = context.applicationContext
}

actual fun defaultStorageDir(): String = appContext.filesDir.absolutePath

actual val debugToolsEnabled: Boolean
    get() = appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
