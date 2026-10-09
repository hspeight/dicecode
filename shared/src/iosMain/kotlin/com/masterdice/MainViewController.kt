package com.masterdice

import androidx.compose.ui.window.ComposeUIViewController
import com.masterdice.ui.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    App()
}
