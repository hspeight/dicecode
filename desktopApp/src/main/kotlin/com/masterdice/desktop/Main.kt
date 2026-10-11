package com.masterdice.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.masterdice.ui.App
import androidx.compose.ui.res.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Master Dice",
        icon = painterResource("app_icon.png"),
        state = rememberWindowState(size = DpSize(480.dp, 860.dp)),
    ) {
        App()
    }
}
