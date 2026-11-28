package com.example.nextclass

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nextclass.ui.MainViewModel
import com.example.nextclass.ui.NextClassRoot
import com.example.nextclass.ui.theme.NextClassTheme

class MainActivity : ComponentActivity() {

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpen(intent)
    }

    private fun handleOpen(intent: android.content.Intent?) {
        intent?.getStringExtra(EXTRA_OPEN)?.let { viewModel.requestOpen(it) }
        intent?.removeExtra(EXTRA_OPEN)
    }

    companion object {
        const val EXTRA_OPEN = "nextclass_open"
        const val OPEN_PLAN = "plan"
        const val OPEN_NOTES = "notes"
        const val OPEN_TASKS = "tasks"
        const val OPEN_REVIEW = "review"
        const val OPEN_FOCUS = "focus"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Fully transparent status and navigation bars, content draws edge to edge.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        handleOpen(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            NextClassTheme(dynamicColor = settings.dynamicColor) {
                NextClassRoot(viewModel)
            }
        }
    }
}
