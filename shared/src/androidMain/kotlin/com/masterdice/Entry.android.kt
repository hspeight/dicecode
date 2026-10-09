package com.masterdice

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.masterdice.ui.App

/** Entry point for the Android app: called by MainActivity in androidApp. */
fun ComponentActivity.showMasterDice() {
    enableEdgeToEdge()
    setContent {
        App()
    }
}
