package com.masterdice.platform

/** Directory where the in-progress game is persisted. */
expect fun defaultStorageDir(): String

/** True only in debug builds — gates the fixed-deal dev tools. */
expect val debugToolsEnabled: Boolean
