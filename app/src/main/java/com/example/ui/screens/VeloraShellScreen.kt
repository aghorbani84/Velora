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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.FazoraCarbonBorder
import com.example.ui.theme.FazoraCarbonElevated
import com.example.ui.theme.FazoraEmeraldOk
import com.example.ui.theme.FazoraGold
import com.example.ui.theme.FazoraGoldBright
import com.example.ui.theme.FazoraGoldDeep
import com.example.ui.theme.FazoraGoldMuted
import com.example.ui.theme.FazoraIvory
import com.example.ui.theme.FazoraObsidian
import com.example.ui.theme.FazoraSoftGold
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
                    VeloraConsoleNavigationRail(
                        visibleDestinations = visibleDestinations,
                        currentDestination = currentDestination,
                        isPersian = isPersian,
                        onNavigate = { dest -> navController.navigateSingleTopTo(dest) }
                    )
                }

                Scaffold(
                    containerColor = FazoraObsidian,
                    topBar = {
                        VeloraStudioTopConsoleBar(
                            isSimpleMode = isSimpleMode,
                            isPersian = isPersian,
                            routeDeviceName = uiState.audioRoute.deviceName,
                            isPlaying = uiState.engineState.isPlaying,
                            onTriggerAutoScan = { permissionLauncher.launch(permissionsToRequest) },
                            onToggleSimpleMode = { viewModel.setSimpleMode(!isSimpleMode) }
                        )
                    },
                    bottomBar = {
                        VeloraBottomConsoleDock(
                            uiState = uiState,
                            currentDestination = currentDestination,
                            visibleDestinations = visibleDestinations,
                            useNavigationRail = useNavigationRail,
                            isPersian = isPersian,
                            onDismissBanner = { viewModel.dismissBanner() },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.skipToNext() },
                            onPrevious = { viewModel.skipToPrevious() },
                            onCycleRepeat = { viewModel.cycleRepeatMode() },
                            onOpenFullPlayer = {
                                navController.navigateSingleTopTo(VeloraDestination.PLAYER)
                            },
                            onNavigate = { dest -> navController.navigateSingleTopTo(dest) }
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        FazoraGoldDeep.copy(alpha = 0.08f),
                                        FazoraObsidian
                                    ),
                                    center = Offset(240f, 80f),
                                    radius = 950f
                                )
                            )
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

@Composable
private fun VeloraStudioTopConsoleBar(
    isSimpleMode: Boolean,
    isPersian: Boolean,
    routeDeviceName: String,
    isPlaying: Boolean,
    onTriggerAutoScan: () -> Unit,
    onToggleSimpleMode: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Surface(
            color = FazoraCarbon.copy(alpha = 0.96f),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FazoraGold.copy(alpha = 0.45f),
                            FazoraCarbonBorder,
                            FazoraGold.copy(alpha = 0.25f)
                        )
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(FazoraGoldBright, FazoraGoldDeep)
                                )
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = FazoraObsidian,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "VELORA",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = FazoraIvory
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (isSimpleMode) {
                                    FazoraCarbonElevated
                                } else {
                                    FazoraGold.copy(alpha = 0.18f)
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.border(
                                    width = 0.8.dp,
                                    color = if (isSimpleMode) {
                                        FazoraCarbonBorder
                                    } else {
                                        FazoraGold.copy(alpha = 0.6f)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                )
                            ) {
                                Text(
                                    text = if (isSimpleMode) "PURE PLAY" else "STUDIO PRO",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSimpleMode) FazoraSoftGold else FazoraGoldBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) FazoraEmeraldOk else FazoraGold)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPersian) {
                                    "خروجی: $routeDeviceName • پردازش ۶۴ بیت"
                                } else {
                                    "$routeDeviceName • 64-Bit DSP"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = FazoraSteel,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onTriggerAutoScan,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(FazoraCarbonElevated)
                            .testTag("top_bar_auto_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Auto-scan media",
                            tint = FazoraGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onToggleSimpleMode,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSimpleMode) FazoraCarbonElevated else FazoraGold,
                            contentColor = if (isSimpleMode) FazoraGold else FazoraObsidian
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("top_bar_mode_switcher_button")
                    ) {
                        Icon(
                            imageVector = if (isSimpleMode) {
                                Icons.Default.Tune
                            } else {
                                Icons.Default.AutoAwesome
                            },
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSimpleMode) {
                                if (isPersian) "استودیو" else "Studio Pro"
                            } else {
                                if (isPersian) "حالت خلوت" else "Pure Play"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VeloraConsoleNavigationRail(
    visibleDestinations: List<VeloraDestination>,
    currentDestination: VeloraDestination,
    isPersian: Boolean,
    onNavigate: (VeloraDestination) -> Unit
) {
    NavigationRail(
        containerColor = FazoraCarbon,
        contentColor = FazoraIvory,
        modifier = Modifier
            .fillMaxHeight()
            .border(1.dp, FazoraCarbonBorder)
            .testTag("fazora_nav_rail")
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        visibleDestinations.forEach { dest ->
            val selected = currentDestination == dest
            val navLabel = if (isPersian) dest.labelFa else dest.label
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(dest) },
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
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = FazoraObsidian,
                    selectedTextColor = FazoraGoldBright,
                    indicatorColor = FazoraGold,
                    unselectedIconColor = FazoraSteel,
                    unselectedTextColor = FazoraSteel
                ),
                modifier = Modifier.testTag("nav_item_${dest.route}")
            )
        }
    }
}

@Composable
private fun VeloraBottomConsoleDock(
    uiState: VeloraUiState,
    currentDestination: VeloraDestination,
    visibleDestinations: List<VeloraDestination>,
    useNavigationRail: Boolean,
    isPersian: Boolean,
    onDismissBanner: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onNavigate: (VeloraDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        if (uiState.statusBannerMessage != null) {
            Surface(
                color = FazoraGoldMuted.copy(alpha = 0.95f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PhiTokens.Space13, vertical = 4.dp)
                    .border(1.dp, FazoraGold, RoundedCornerShape(14.dp))
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
                    TextButton(onClick = onDismissBanner) {
                        Text("OK", color = FazoraGoldBright, fontWeight = FontWeight.Bold)
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
                onTogglePlayPause = onTogglePlayPause,
                onNext = onNext,
                onCycleRepeat = onCycleRepeat,
                onOpenFullPlayer = onOpenFullPlayer,
                onPrevious = onPrevious
            )
        }

        if (!useNavigationRail) {
            Surface(
                color = FazoraCarbon,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 0.8.dp, color = FazoraCarbonBorder)
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = FazoraIvory,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fazora_bottom_nav")
                ) {
                    visibleDestinations.forEach { dest ->
                        val selected = currentDestination == dest
                        val navLabel = if (isPersian) dest.labelFa else dest.label
                        NavigationBarItem(
                            selected = selected,
                            onClick = { onNavigate(dest) },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = navLabel
                                )
                            },
                            label = {
                                Text(
                                    text = navLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FazoraObsidian,
                                selectedTextColor = FazoraGoldBright,
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
    }
}
