package com.masterdice.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.masterdice.platform.installAndroidContext
import com.masterdice.showMasterDice

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installAndroidContext(this)
        showMasterDice()
    }
}
