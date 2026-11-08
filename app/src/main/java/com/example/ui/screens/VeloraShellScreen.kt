package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.components.MiniPlayerBar
import com.example.ui.navigation.VeloraDestination
import com.example.ui.navigation.VeloraNavGraph
import com.example.ui.navigation.navigateSingleTopTo
import com.example.ui.theme.FazoraCarbon
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSteel
import com.example.ui.theme.PhiTokens
import com.example.ui.viewmodel.VeloraUiState
import com.example.ui.viewmodel.VeloraViewModel

@Composable
fun VeloraShellScreen(
    navController: NavHostController,
    viewModel: VeloraViewModel,
    uiState: VeloraUiState
) {
    val context = LocalContext.current
    val isPersian = uiState.settings.appLanguageCode == "fa"
    val isSimpleMode = uiState.settings.isSimpleMode
    val homeDestination = if (isSimpleMode) VeloraDestination.MUSIC else VeloraDestination.LIBRARY
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = VeloraDestination.fromRoute(navBackStackEntry?.destination?.route)

    val visibleDestinations = if (isSimpleMode) {
        listOf(
            VeloraDestination.MUSIC,
            VeloraDestination.VIDEO,
            VeloraDestination.RADIO,
            VeloraDestination.SETTINGS
        )
    } else {
        VeloraDestination.entries
    }

    LaunchedEffect(isSimpleMode) {
        if (isSimpleMode && currentDestination !in visibleDestinations &&
            currentDestination != VeloraDestination.PLAYER
        ) {
            navController.navigateSingleTopTo(VeloraDestination.MUSIC)
        }
    }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            com.example.ui.permissions.PermissionController.requiredMediaPermissions() +
                Manifest.permission.POST_NOTIFICATIONS
        } else {
            com.example.ui.permissions.PermissionController.requiredMediaPermissions()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.autoScanDeviceMedia()
    }

    LaunchedEffect(uiState.settings.autoScanOnStartup) {
        if (uiState.settings.autoScanOnStartup) {
            val allGranted = permissionsToRequest.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                viewModel.autoScanDeviceMedia()
            }
        }
    }

    BackHandler(enabled = currentDestination != homeDestination) {
        navController.navigateSingleTopTo(homeDestination)
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(FazoraObsidian)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val useNavigationRail = maxWidth >= 680.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = FazoraCarbon,
                        contentColor = FazoraIvory,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("fazora_nav_rail")
                    ) {
                        visibleDestinations.forEach { dest ->
                            val selected = currentDestination == dest
                            val navLabel = if (isPersian) dest.labelFa else dest.label
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navController.navigateSingleTopTo(dest) },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = navLabel
                                    )
                                },
                                label = {
                                    Text(
                                        text = navLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = FazoraObsidian,
                                    selectedTextColor = FazoraGold,
                                    indicatorColor = FazoraGold,
                                    unselectedIconColor = FazoraSteel,
                                    unselectedTextColor = FazoraSteel
                                ),
                                modifier = Modifier.testTag("nav_item_${dest.route}")
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = FazoraObsidian,
                    topBar = {
                        Surface(
                            color = FazoraCarbon,
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "VELORA",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = FazoraGold
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = FazoraCarbonElevated,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = if (isSimpleMode) "SIMPLE" else "STUDIO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSimpleMode) FazoraIvory else FazoraGold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { permissionLauncher.launch(permissionsToRequest) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("top_bar_auto_scan_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Auto-scan media",
                                            tint = FazoraGold
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.setSimpleMode(!isSimpleMode) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSimpleMode) FazoraCarbonElevated else FazoraGold,
                                            contentColor = if (isSimpleMode) FazoraGold else FazoraObsidian
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("top_bar_mode_switcher_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isSimpleMode) Icons.Default.Tune else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isSimpleMode) {
                                                if (isPersian) "حالت پیشرفته" else "Studio Pro"
                                            } else {
                                                if (isPersian) "حالت ساده" else "Simple Mode"
                                            },
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            if (uiState.statusBannerMessage != null) {
                                Surface(
                                    color = FazoraGoldMuted,
                                    shape = RoundedCornerShape(PhiTokens.CornerRadius8),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = PhiTokens.Space13, vertical = 4.dp)
                                        .border(1.dp, FazoraGold, RoundedCornerShape(PhiTokens.CornerRadius8))
                                        .testTag("status_banner")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = uiState.statusBannerMessage.orEmpty(),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = FazoraIvory,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { viewModel.dismissBanner() }) {
                                            Text("OK", color = FazoraGold)
                                        }
                                    }
                                }
                            }

                            if (currentDestination != VeloraDestination.PLAYER &&
                                currentDestination != VeloraDestination.VIDEO
                            ) {
                                MiniPlayerBar(
                                    engineState = uiState.engineState,
                                    repeatMode = uiState.queue.repeatMode,
                                    onTogglePlayPause = { viewModel.togglePlayPause() },
                                    onNext = { viewModel.skipToNext() },
                                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                                    onOpenFullPlayer = {
                                        navController.navigateSingleTopTo(VeloraDestination.PLAYER)
                                    },
                                    onPrevious = { viewModel.skipToPrevious() }
                                )
                            }

                            if (!useNavigationRail) {
                                NavigationBar(
                                    containerColor = FazoraCarbon,
                                    contentColor = FazoraIvory,
                                    tonalElevation = 8.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("fazora_bottom_nav")
                                ) {
                                    visibleDestinations.forEach { dest ->
                                        val selected = currentDestination == dest
                                        val navLabel = if (isPersian) dest.labelFa else dest.label
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navController.navigateSingleTopTo(dest) },
                                            icon = {
                                                Icon(
                                                    imageVector = dest.icon,
                                                    contentDescription = navLabel
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = navLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = FazoraObsidian,
                                                selectedTextColor = FazoraGold,
                                                indicatorColor = FazoraGold,
                                                unselectedIconColor = FazoraSteel,
                                                unselectedTextColor = FazoraSteel
                                            ),
                                            modifier = Modifier.testTag("nav_item_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        VeloraNavGraph(
                            navController = navController,
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }
                }
            }
        }
    }
}
