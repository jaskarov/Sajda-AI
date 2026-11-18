package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.CoachMode
import com.example.domain.model.FallbackReason
import com.example.domain.model.PrayerTrack
import com.example.domain.model.lText
import com.example.ui.screens.ActivePrayerScreen
import com.example.ui.screens.CalibrationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LearnScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.WeeklyProgressScreen
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PrayerCoachViewModel
import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data class PermissionsRoute(val fromSettings: Boolean = false)

@Serializable
data object MainTabsRoute

@Serializable
data class CalibrationRoute(val trackId: String, val qazoTypeId: String = "")

@Serializable
data class ActivePrayerRoute(
    val trackId: String,
    val modeName: String,
    val fallbackReasonName: String,
    val qazoTypeId: String = ""
)

@Serializable
data object SettingsRoute

@Composable
fun SajdaNavGraph(
    mainViewModel: MainViewModel = viewModel()
) {
    val mainUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    if (!mainUiState.isReady) return

    val navController = rememberNavController()
    val startDest: Any = if (mainUiState.settings.onboardingCompleted) {
        MainTabsRoute
    } else {
        OnboardingRoute
    }

    val baseDensity = LocalDensity.current
    val effectiveDensity = if (mainUiState.settings.largeTextMode) {
        Density(
            density = baseDensity.density,
            fontScale = (baseDensity.fontScale * 1.18f).coerceAtMost(1.6f)
        )
    } else {
        baseDensity
    }

    CompositionLocalProvider(LocalDensity provides effectiveDensity) {
    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        composable<OnboardingRoute> {
            var pendingSeated by remember { mutableStateOf(mainUiState.settings.seatedPrayerMode) }
            var pendingLargeText by remember { mutableStateOf(mainUiState.settings.largeTextMode) }
            OnboardingScreen(
                initialGender = mainUiState.settings.gender,
                initialScript = mainUiState.settings.script,
                initialCityId = mainUiState.settings.selectedCityId,
                initialSeatedMode = pendingSeated,
                initialLargeTextMode = pendingLargeText,
                onAccessibilitySelected = { seated, largeText ->
                    pendingSeated = seated
                    pendingLargeText = largeText
                },
                onComplete = { gender, script, cityId ->
                    mainViewModel.completeOnboarding(
                        gender = gender,
                        script = script,
                        cityId = cityId,
                        seatedPrayerMode = pendingSeated,
                        largeTextMode = pendingLargeText
                    )
                    navController.navigate(PermissionsRoute(fromSettings = false)) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<PermissionsRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<PermissionsRoute>()
            PermissionsScreen(
                script = mainUiState.settings.script,
                isFromSettings = route.fromSettings,
                onContinue = {
                    if (route.fromSettings) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(MainTabsRoute) {
                            popUpTo<PermissionsRoute> { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<MainTabsRoute> {
            var selectedTab by rememberSaveable { mutableIntStateOf(0) }
            var learnInitialTab by rememberSaveable { mutableIntStateOf(0) }
            val script = mainUiState.settings.script

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Home
                                    else Icons.Outlined.Home,
                                    contentDescription = null
                                )
                            },
                            label = { Text(lText("Asosiy", "Асосий").get(script)) },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 1) Icons.Filled.MenuBook
                                    else Icons.Outlined.MenuBook,
                                    contentDescription = null
                                )
                            },
                            label = { Text(lText("O‘rganish", "Ўрганиш").get(script)) },
                            modifier = Modifier.testTag("nav_learn")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.BarChart
                                    else Icons.Outlined.BarChart,
                                    contentDescription = null
                                )
                            },
                            label = { Text(lText("Statistika", "Статистика").get(script)) },
                            modifier = Modifier.testTag("nav_stats")
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.Explore
                                    else Icons.Outlined.Explore,
                                    contentDescription = null
                                )
                            },
                            label = { Text(lText("Qibla", "Қибла").get(script)) },
                            modifier = Modifier.testTag("nav_qibla")
                        )
                    }
                }
            ) { innerPadding ->
                when (selectedTab) {
                    0 -> HomeScreen(
                        uiState = mainUiState,
                        onStartPrayer = { track ->
                            navController.navigate(CalibrationRoute(track.id))
                        },
                        onToggleAlarm = { mainViewModel.togglePrayerAlarm(it) },
                        onToggleCompleted = { mainViewModel.togglePrayerCompleted(it) },
                        onCitySelected = { mainViewModel.setCityId(it) },
                        onOpenSettings = { navController.navigate(SettingsRoute) },
                        onOpenWeeklyStats = { selectedTab = 2 },
                        onOpenWuduGuide = {
                            learnInitialTab = 1
                            selectedTab = 1
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    1 -> LearnScreen(
                        uiState = mainUiState,
                        onPlayAudio = { prompt, assetId ->
                            mainViewModel.speakSample(prompt, assetId)
                        },
                        onStartTrack = { track ->
                            navController.navigate(CalibrationRoute(track.id))
                        },
                        initialTab = learnInitialTab,
                        onTabChanged = { learnInitialTab = it },
                        onToggleTahoratAutoPlay = { steps ->
                            if (mainUiState.tahoratAutoPlayActive) {
                                mainViewModel.stopTahoratAutoPlay()
                            } else {
                                mainViewModel.startTahoratAutoPlay(steps)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    2 -> WeeklyProgressScreen(
                        uiState = mainUiState,
                        onShiftWeek = mainViewModel::shiftWeek,
                        onTogglePrayerForDay = mainViewModel::togglePrayerCompletedForDay,
                        onIncrementQazo = mainViewModel::incrementQazo,
                        onDecrementQazo = mainViewModel::decrementQazo,
                        onAddQazoYears = mainViewModel::addQazoYears,
                        onPrayQazo = { qazoType ->
                            navController.navigate(
                                CalibrationRoute(
                                    trackId = qazoType.toPrayerTrack().id,
                                    qazoTypeId = qazoType.id
                                )
                            )
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                    else -> {
                        val compassState by mainViewModel.compassUiState.collectAsStateWithLifecycle()
                        QiblaScreen(
                            uiState = mainUiState,
                            compassState = compassState,
                            onDetectNearestCity = { mainViewModel.detectNearestCityFromCoarseLocation() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }

        composable<CalibrationRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<CalibrationRoute>()
            val coachViewModel: PrayerCoachViewModel = viewModel(backStackEntry)
            val coachState by coachViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(route.trackId, route.qazoTypeId) {
                coachViewModel.prepareTrack(
                    track = PrayerTrack.fromId(route.trackId),
                    qazoTypeId = route.qazoTypeId
                )
            }

            CalibrationScreen(
                uiState = coachState,
                onPoseResult = coachViewModel::onPoseSnapshot,
                onSpeakCalibrationPrompt = coachViewModel::speakCalibrationPrompt,
                onStartActiveSession = { mode, reason ->
                    navController.navigate(
                        ActivePrayerRoute(
                            trackId = route.trackId,
                            modeName = mode.name,
                            fallbackReasonName = reason.name,
                            qazoTypeId = route.qazoTypeId
                        )
                    ) {
                        popUpTo<CalibrationRoute> { inclusive = true }
                    }
                },
                onSeatedModeToggle = coachViewModel::setSeatedPrayerMode,
                onExit = { navController.popBackStack() }
            )
        }

        composable<ActivePrayerRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ActivePrayerRoute>()
            val coachViewModel: PrayerCoachViewModel = viewModel(backStackEntry)
            val coachState by coachViewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(route.trackId, route.modeName, route.qazoTypeId) {
                val track = PrayerTrack.fromId(route.trackId)
                val mode = runCatching { CoachMode.valueOf(route.modeName) }
                    .getOrDefault(CoachMode.CAMERA_AI)
                val reason = runCatching { FallbackReason.valueOf(route.fallbackReasonName) }
                    .getOrDefault(FallbackReason.NONE)
                coachViewModel.startActivePrayerSession(
                    track = track,
                    mode = mode,
                    fallbackReason = reason,
                    qazoTypeId = route.qazoTypeId
                )
            }

            ActivePrayerScreen(
                uiState = coachState,
                onPoseResult = coachViewModel::onPoseSnapshot,
                onTogglePause = coachViewModel::togglePause,
                onNextStep = coachViewModel::advanceToNextStep,
                onStartNextTrack = coachViewModel::startNextContinuationTrack,
                onExtendContinuation = { coachViewModel.extendContinuationCountdown(10) },
                onSwitchToAudioOnly = {
                    coachViewModel.switchToAudioOnly(FallbackReason.USER_SELECTED)
                },
                onSwitchToCamera = coachViewModel::switchToCameraMode,
                onExit = {
                    coachViewModel.cancelContinuationCountdown()
                    navController.popBackStack()
                }
            )
        }

        composable<SettingsRoute> {
            SettingsScreen(
                uiState = mainUiState,
                onGenderChange = mainViewModel::setGender,
                onScriptChange = mainViewModel::setScript,
                onCityChange = mainViewModel::setCityId,
                onPreferOfflineMp3Toggle = mainViewModel::setPreferOfflineMp3,
                onVoiceSpeedChange = mainViewModel::setVoiceSpeed,
                onDarkThresholdChange = mainViewModel::setDarkRoomThreshold,
                onSeatedModeToggle = mainViewModel::setSeatedPrayerMode,
                onLargeTextModeToggle = mainViewModel::setLargeTextMode,
                onLowPowerCameraToggle = mainViewModel::setLowPowerCameraMode,
                onAutoBoostVolumeToggle = mainViewModel::setAutoBoostPrayerVolume,
                onAcousticChimeToggle = mainViewModel::setAcousticChimeEnabled,
                onSyncCalendarNow = mainViewModel::syncPrayerCalendarNow,
                onExportBackupUri = mainViewModel::exportBackupToUri,
                onImportBackupUri = mainViewModel::importBackupFromUri,
                onClearBackupMessage = mainViewModel::clearBackupMessage,
                onOpenPermissions = {
                    navController.navigate(PermissionsRoute(fromSettings = true))
                },
                onTestAudio = {
                    val sample = lText(
                        "Assalomu alaykum. Oflayn Qori qiroati tayyor.",
                        "Ассалому алайкум. Офлайн Қори қироати тайёр."
                    ).get(mainUiState.settings.script)
                    mainViewModel.speakSample(sample, "fotiha")
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
    }
}
