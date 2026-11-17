package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.ExperienceTier
import com.example.ui.components.MiniPlayerBar
import com.example.ui.navigation.VeloraDestination
import com.example.ui.navigation.VeloraNavGraph
import com.example.ui.navigation.navigateSingleTopTo
import com.example.ui.theme.PhiTokens
import com.example.ui.theme.VeloraTheme
import com.example.ui.viewmodel.VeloraViewModel
import com.example.ui.viewmodel.setExperienceTier

@Composable
fun VeloraShellScreen(
    viewModel: VeloraViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val positionState by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = VeloraDestination.fromRoute(backStackEntry?.destination?.route)
    val context = LocalContext.current

    val startupPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val autoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.autoScanDeviceMedia()
    }

    var didRunStartupAutoScan by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.settings.autoScanOnStartup) {
        if (!didRunStartupAutoScan && uiState.settings.autoScanOnStartup) {
            didRunStartupAutoScan = true
            val allGranted = startupPermissions.all { perm ->
                ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
            }
            if (allGranted) {
                viewModel.autoScanDeviceMedia()
            } else {
                autoPermissionLauncher.launch(startupPermissions)
            }
        }
    }

    val tier = uiState.settings.experienceTier
    val homeDestination = if (tier == ExperienceTier.SIMPLE) {
        VeloraDestination.MUSIC
    } else {
        VeloraDestination.LIBRARY
    }

    val visibleDestinations = remember(tier) {
        when (tier) {
            ExperienceTier.SIMPLE -> listOf(
                VeloraDestination.MUSIC,
                VeloraDestination.VIDEO,
                VeloraDestination.PLAYER,
                VeloraDestination.SETTINGS
            )
            ExperienceTier.STANDARD, ExperienceTier.PRO -> VeloraDestination.entries
        }
    }

    LaunchedEffect(tier) {
        if (tier == ExperienceTier.SIMPLE &&
            currentDestination !in visibleDestinations &&
            navController.currentDestination != null
        ) {
            navController.navigateSingleTopTo(VeloraDestination.MUSIC)
        }
    }

    BackHandler(enabled = currentDestination != homeDestination) {
        navController.navigateSingleTopTo(homeDestination)
    }

    val isPersian = uiState.settings.appLanguageCode == "fa"
    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    VeloraTheme(
        paletteMode = uiState.settings.themePaletteMode,
        isPersian = isPersian
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("velora_shell_root")
            ) {
                val isWideScreen = maxWidth >= PhiTokens.WideScreenBreakpoint

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        VeloraTopHeaderBar(
                            trackCount = uiState.allTracks.size,
                            tier = tier,
                            isPersian = isPersian,
                            bannerMessage = uiState.statusBannerMessage,
                            onDismissBanner = { viewModel.dismissBanner() },
                            onCycleLanguage = {
                                viewModel.setAppLanguageCode(if (isPersian) "en" else "fa")
                            },
                            onSelectTier = { viewModel.setExperienceTier(it) }
                        )
                    },
                    bottomBar = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MiniPlayerBar(
                                engineState = uiState.engineState,
                                positionState = positionState,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSkipNext = { viewModel.skipToNext() },
                                onOpenPlayer = {
                                    navController.navigateSingleTopTo(VeloraDestination.PLAYER)
                                }
                            )
                            if (!isWideScreen) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    visibleDestinations.forEach { dest ->
                                        val selected = currentDestination == dest
                                        val label = if (isPersian) dest.labelFa else dest.label
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { navController.navigateSingleTopTo(dest) },
                                            icon = { Icon(dest.icon, contentDescription = label) },
                                            label = { Text(label, maxLines = 1) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primary,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = Modifier.testTag("nav_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .testTag("side_nav_rail")
                            ) {
                                visibleDestinations.forEach { dest ->
                                    val selected = currentDestination == dest
                                    val label = if (isPersian) dest.labelFa else dest.label
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navController.navigateSingleTopTo(dest) },
                                        icon = { Icon(dest.icon, contentDescription = label) },
                                        label = { Text(label) },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primary,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag("nav_${dest.route}")
                                    )
                                }
                            }
                        }
                        Box(modifier = Modifier.fillMaxSize()) {
                            VeloraNavGraph(
                                navController = navController,
                                viewModel = viewModel,
                                uiState = uiState,
                                positionState = positionState,
                                startDestination = homeDestination
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VeloraTopHeaderBar(
    trackCount: Int,
    tier: ExperienceTier,
    isPersian: Boolean,
    bannerMessage: String?,
    onDismissBanner: () -> Unit,
    onCycleLanguage: () -> Unit,
    onSelectTier: (ExperienceTier) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PhiTokens.Space21, vertical = PhiTokens.Space13)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(PhiTokens.IconSize24)
                )
                Spacer(modifier = Modifier.width(PhiTokens.Space8))
                Column {
                    Text(
                        text = "VELORA",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isPersian) tier.labelFa else tier.labelEn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PhiTokens.Space8)
            ) {
                val nextTier = when (tier) {
                    ExperienceTier.SIMPLE -> ExperienceTier.STANDARD
                    ExperienceTier.STANDARD -> ExperienceTier.PRO
                    ExperienceTier.PRO -> ExperienceTier.SIMPLE
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable { onSelectTier(nextTier) }
                        .testTag("shell_mode_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(PhiTokens.IconSize16)
                        )
                        Spacer(modifier = Modifier.width(PhiTokens.Space4))
                        Text(
                            text = tier.name,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(PhiTokens.CornerRadius13),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onCycleLanguage)
                        .testTag("shell_language_toggle_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = PhiTokens.Space13, vertical = PhiTokens.Space8)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Switch Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(PhiTokens.IconSize16)
                        )
                        Spacer(modifier = Modifier.width(PhiTokens.Space4))
                        Text(
                            text = if (isPersian) "EN" else "فا",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Text(
                    text = "$trackCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.testTag("shell_track_count_badge")
                )
            }
        }

        if (!bannerMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(PhiTokens.BorderHairline, MaterialTheme.colorScheme.primary)
                    .testTag("status_banner")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = PhiTokens.Space21, vertical = PhiTokens.Space8)
                ) {
                    Text(
                        text = bannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissBanner,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dismiss_banner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss Banner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
