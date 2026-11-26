package dev.velocity.oneui.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dev.velocity.oneui.core.BuildConstants

class MainActivity : ComponentActivity() {

    private lateinit var state: UiState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = UiState(this)
        setContent {
            VelocityTheme {
                VelocityApp(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        state.refresh()
    }
}
