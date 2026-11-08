package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
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

        setContent {
            VeloraRootApp()
        }
    }
}

@Composable
fun VeloraRootApp(
    viewModel: VeloraViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    VeloraTheme(paletteMode = uiState.settings.themePaletteMode) {
        VeloraShellScreen(
            navController = navController,
            viewModel = viewModel,
            uiState = uiState
        )
    }
}
