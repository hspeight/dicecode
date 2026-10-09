package com.masterdice.platform

import android.content.Context

private lateinit var appContext: Context

/** Must be called once from the Android Application or Activity. */
fun installAndroidContext(context: Context) {
    appContext = context.applicationContext
}

actual fun defaultStorageDir(): String = appContext.filesDir.absolutePath
