package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.VeloraShellScreen
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.VeloraViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialMediaUri = extractIncomingMediaTarget(intent)

        setContent {
            VeloraRootApp(initialMediaUri = initialMediaUri)
        }
    }

    private fun extractIncomingMediaTarget(incoming: Intent?): String? {
        if (incoming == null) return null
        return when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.dataString
            Intent.ACTION_SEND -> {
                incoming.getStringExtra(Intent.EXTRA_TEXT) ?: run {
                    val streamUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        incoming.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    streamUri?.toString()
                }
            }
            else -> null
        }
    }
}

@Composable
fun VeloraRootApp(
    viewModel: VeloraViewModel = koinViewModel(),
    initialMediaUri: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    LaunchedEffect(initialMediaUri) {
        if (!initialMediaUri.isNullOrBlank()) {
            viewModel.handleIncomingMediaIntent(initialMediaUri)
        }
    }

    VeloraTheme(paletteMode = uiState.settings.themePaletteMode) {
        VeloraShellScreen(
            navController = navController,
            viewModel = viewModel,
            uiState = uiState
        )
    }
}
