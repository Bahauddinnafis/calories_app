package com.nafis.calories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.nafis.calories.ui.history.HistoryScreen
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.navigation.AppTab
import com.nafis.calories.ui.navigation.CaloriesBottomBar
import com.nafis.calories.ui.onboarding.OnboardingScreen
import com.nafis.calories.ui.permission.CameraPermissionScreen
import com.nafis.calories.ui.platform.rememberCameraPermissionState
import com.nafis.calories.ui.profile.ProfileScreen
import com.nafis.calories.ui.profile.ProfileViewModel
import com.nafis.calories.ui.profile.TargetScreen
import com.nafis.calories.ui.scan.HomeScreen
import com.nafis.calories.ui.scan.HomeViewModel
import com.nafis.calories.ui.scan.ScanFlow
import com.nafis.calories.ui.scan.ScanUiState
import com.nafis.calories.ui.scan.ScanViewModel
import com.nafis.calories.ui.theme.CaloriesTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CaloriesAppScreen() {
    CaloriesTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val profileViewModel = koinViewModel<ProfileViewModel>()
            val savedProfile by profileViewModel.profile.collectAsStateWithLifecycle()
            val cameraPermission = rememberCameraPermissionState()
            var forceOnboarding by rememberSaveable { mutableStateOf(false) }

            when {
                forceOnboarding || savedProfile?.isOnboardingCompleted != true -> OnboardingScreen(
                    initialProfile = savedProfile,
                    onComplete = { newProfile ->
                        profileViewModel.saveProfile(newProfile)
                        forceOnboarding = false
                    }
                )
                !cameraPermission.isGranted -> CameraPermissionScreen(
                    permanentlyDenied = cameraPermission.isPermanentlyDenied,
                    onRequest = cameraPermission::request
                )
                else -> MainApp(
                    profileViewModel = profileViewModel,
                    onStartOnboarding = { forceOnboarding = true }
                )
            }
        }
    }
}

@Composable
private fun MainApp(
    profileViewModel: ProfileViewModel,
    onStartOnboarding: () -> Unit
) {
    val scanViewModel = koinViewModel<ScanViewModel>()
    val homeViewModel = koinViewModel<HomeViewModel>()
    val historyViewModel = koinViewModel<HistoryViewModel>()

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.Home) }
    val scanState by scanViewModel.uiState.collectAsStateWithLifecycle()
    val dailyTargets by profileViewModel.dailyTargets.collectAsStateWithLifecycle()

    LifecycleResumeEffect(homeViewModel) {
        homeViewModel.refreshWater()
        onPauseOrDispose { }
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = selectedTab != AppTab.Home || scanState !is ScanUiState.Home,
        onBackCompleted = {
            when {
                scanState is ScanUiState.Editing -> scanViewModel.cancelCorrection()
                scanState !is ScanUiState.Home -> scanViewModel.resetState()
                else -> selectedTab = AppTab.Home
            }
        }
    )

    val showBottomBar = scanState is ScanUiState.Home || scanState is ScanUiState.Success || scanState is ScanUiState.Warning

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CaloriesBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        if (scanState !is ScanUiState.Home) scanViewModel.resetState()
                    },
                    onScanClick = scanViewModel::openScanner
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (scanState !is ScanUiState.Home) {
                ScanFlow(state = scanState, viewModel = scanViewModel)
            } else when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    viewModel = homeViewModel,
                    onEntryClick = scanViewModel::showHistoryDetail,
                    onOpenProfile = { selectedTab = AppTab.Profile },
                    onOpenHistory = { selectedTab = AppTab.History }
                )
                AppTab.History -> HistoryScreen(
                    viewModel = historyViewModel,
                    onHistoryItemClick = { entry, image ->
                        scanViewModel.showHistoryDetail(entry, image)
                        selectedTab = AppTab.Home
                    },
                    dailyTargetKcal = dailyTargets.energyKcal
                )
                AppTab.Target -> TargetScreen(viewModel = profileViewModel, onEditProfiling = onStartOnboarding)
                AppTab.Profile -> ProfileScreen(viewModel = profileViewModel, onEditProfiling = onStartOnboarding)
            }
        }
    }
}