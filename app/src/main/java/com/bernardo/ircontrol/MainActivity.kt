package com.bernardo.ircontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.bernardo.ircontrol.ui.App
import com.bernardo.ircontrol.ui.RemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state = remember { AppState(applicationContext) }
            RemoteTheme { App(state) }
        }
    }
}
