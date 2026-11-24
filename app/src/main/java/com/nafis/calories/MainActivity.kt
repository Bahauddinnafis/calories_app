package com.nafis.calories

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.history.HistoryScreen
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.onboarding.OnboardingScreen
import com.nafis.calories.ui.profile.ProfileScreen
import com.nafis.calories.ui.profile.ProfileViewModel
import com.nafis.calories.ui.profile.TargetScreen
import com.nafis.calories.ui.scan.CorrectionScreen
import com.nafis.calories.ui.scan.HomeScreen
import com.nafis.calories.ui.scan.MultiModeScannerScreen
import com.nafis.calories.ui.scan.NutritionUnavailableScreen
import com.nafis.calories.ui.scan.ResultScreen
import com.nafis.calories.ui.scan.ScanUiState
import com.nafis.calories.ui.scan.ScanViewModel
import com.nafis.calories.ui.theme.CaloriesTheme
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.PrimarySoft
import com.nafis.calories.ui.theme.Spacing

class MainActivity : ComponentActivity() {

    private lateinit var scanViewModel: ScanViewModel
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var profileViewModel: ProfileViewModel

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        scanViewModel = ScanViewModel(this)
        historyViewModel = HistoryViewModel(this)
        profileViewModel = ProfileViewModel(this)

        setContent {
            CaloriesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

                    val savedProfile by profileViewModel.profile.collectAsState()
                    var forceOnboarding by remember { mutableStateOf(false) }

                    val showOnboarding = forceOnboarding || savedProfile == null || !(savedProfile?.isOnboardingCompleted ?: false)

                    if (showOnboarding) {
                        OnboardingScreen(
                            initialProfile = savedProfile,
                            onComplete = { newProfile ->
                                profileViewModel.saveProfile(newProfile)
                                forceOnboarding = false
                            }
                        )
                    } else if (cameraPermissionState.status.isGranted) {
                        MainApp(
                            scanViewModel = scanViewModel,
                            historyViewModel = historyViewModel,
                            profileViewModel = profileViewModel,
                            onStartOnboarding = { forceOnboarding = true }
                        )
                    } else {
                        var hasRequested by rememberSaveable { mutableStateOf(false) }
                        val permanentlyDenied = hasRequested && !cameraPermissionState.status.shouldShowRationale
                        CameraPermissionScreen(
                            permanentlyDenied = permanentlyDenied,
                            onRequest = {
                                if (permanentlyDenied) {
                                    openAppSettings(this@MainActivity)
                                } else {
                                    hasRequested = true
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Beranda", Icons.Filled.Home)
    object History : Screen("history", "Riwayat", Icons.AutoMirrored.Filled.List)
    object Target : Screen("target", "Target", Icons.Default.TrackChanges)
    object Profile : Screen("profile", "Profil", Icons.Filled.Person)
}

@Composable
fun MainApp(
    scanViewModel: ScanViewModel,
    historyViewModel: HistoryViewModel,
    profileViewModel: ProfileViewModel,
    onStartOnboarding: () -> Unit
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scanUiState by scanViewModel.uiState.collectAsState()
    val dailyTargets by profileViewModel.dailyTargets.collectAsState()

    val isMainHome = (selectedScreen == Screen.Home) && (scanUiState is ScanUiState.Home)
    BackHandler(enabled = !isMainHome) {
        if (scanUiState is ScanUiState.Editing) {
            scanViewModel.cancelCorrection()
        } else if (scanUiState !is ScanUiState.Home) {
            scanViewModel.resetState()
        } else if (selectedScreen != Screen.Home) {
            selectedScreen = Screen.Home
        }
    }

    val showBottomBar = when (scanUiState) {
        is ScanUiState.Home -> true
        is ScanUiState.Success -> true
        is ScanUiState.Warning -> true
        else -> false
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                CustomBottomBar(
                    selectedScreen = selectedScreen,
                    onScreenSelected = { screen ->
                        selectedScreen = screen
                        if (scanUiState !is ScanUiState.Home) {
                            scanViewModel.resetState()
                        }
                    },
                    onScanClick = {
                        scanViewModel.navigateToRealTimeScan()
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            if (scanUiState !is ScanUiState.Home) {
                ScanApp(
                    viewModel = scanViewModel,
                    historyViewModel = historyViewModel,
                    dailyTargets = dailyTargets,
                    onOpenProfile = { selectedScreen = Screen.Profile }
                )
            } else {
                when (selectedScreen) {
                    Screen.Home -> ScanApp(
                        viewModel = scanViewModel,
                        historyViewModel = historyViewModel,
                        dailyTargets = dailyTargets,
                        onOpenProfile = { selectedScreen = Screen.Profile }
                    )
                    Screen.History -> HistoryScreen(
                        viewModel = historyViewModel,
                        onHistoryItemClick = { history, bitmap ->
                            val scanResult = HistoryMapper.toDomain(history)
                            scanViewModel.showHistoryDetail(scanResult, bitmap)
                            selectedScreen = Screen.Home
                        },
                        dailyTargetKcal = dailyTargets.energyKcal
                    )
                    Screen.Target -> TargetScreen(
                        viewModel = profileViewModel,
                        onEditProfiling = onStartOnboarding
                    )
                    Screen.Profile -> ProfileScreen(
                        viewModel = profileViewModel,
                        onEditProfiling = onStartOnboarding
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomBar(
    selectedScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    onScanClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavBarItem(
                    screen = Screen.Home,
                    selected = selectedScreen == Screen.Home,
                    onClick = { onScreenSelected(Screen.Home) },
                    modifier = Modifier.weight(1f)
                )
                NavBarItem(
                    screen = Screen.History,
                    selected = selectedScreen == Screen.History,
                    onClick = { onScreenSelected(Screen.History) },
                    modifier = Modifier.weight(1f)
                )
                // Spacer for center floating FAB
                Spacer(modifier = Modifier.weight(1f))
                NavBarItem(
                    screen = Screen.Target,
                    selected = selectedScreen == Screen.Target,
                    onClick = { onScreenSelected(Screen.Target) },
                    modifier = Modifier.weight(1f)
                )
                NavBarItem(
                    screen = Screen.Profile,
                    selected = selectedScreen == Screen.Profile,
                    onClick = { onScreenSelected(Screen.Profile) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Center Floating Scan FAB
        Surface(
            modifier = Modifier
                .size(58.dp)
                .offset(y = (-4).dp)
                .border(4.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(20.dp))
                .clickable { onScanClick() },
            shape = RoundedCornerShape(20.dp),
            color = Primary,
            shadowElevation = 6.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.CenterFocusWeak,
                    contentDescription = "Scan Makanan",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    screen: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) Primary else Color(0xFF94A3B8)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) PrimarySoft else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = screen.icon,
                contentDescription = screen.label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = screen.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            fontSize = 11.sp
        )
    }
}

@Composable
fun ScanApp(
    viewModel: ScanViewModel,
    historyViewModel: HistoryViewModel,
    dailyTargets: DailyTargets,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            bitmap?.let { bmp ->
                viewModel.scanFood(bmp)
            }
        }
    }

    when (val state = uiState) {
        is ScanUiState.Home -> {
            HomeScreen(
                onCameraClick = { viewModel.navigateToCamera() },
                onUploadClick = { imagePickerLauncher.launch("image/*") },
                onRealtimeScanClick = { viewModel.navigateToRealTimeScan() },
                historyViewModel = historyViewModel,
                onHistoryItemClick = { history, bitmap ->
                    val scanResult = HistoryMapper.toDomain(history)
                    viewModel.showHistoryDetail(scanResult, bitmap)
                },
                dailyTargets = dailyTargets,
                onSetTargetClick = onOpenProfile,
                scanViewModel = viewModel
            )
        }
        is ScanUiState.Camera -> {
            MultiModeScannerScreen(
                viewModel = viewModel,
                onBack = { viewModel.resetState() }
            )
        }
        is ScanUiState.RealTimeScan -> {
            MultiModeScannerScreen(
                viewModel = viewModel,
                onBack = { viewModel.resetState() }
            )
        }
        is ScanUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Menghitung kalori…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biasanya hanya butuh beberapa detik",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is ScanUiState.Success -> {
            val scanId = state.scanResult.scanId
            ResultScreen(
                scanResult = state.scanResult,
                capturedImage = state.capturedImage,
                onScanAgain = { viewModel.resetState() },
                primaryActionText = if (state.fromHistory) "Tutup" else "Simpan ke Catatan",
                dailyEnergyKcal = dailyTargets.energyKcal,
                loggedAt = state.scanResult.timestamp.takeIf { state.fromHistory },
                onPortionChanged = { viewModel.updateConsumedPortion(scanId, it) },
                onMealTypeChanged = { viewModel.updateMealType(scanId, it) },
                onCorrect = { viewModel.startCorrection() }
            )
        }
        is ScanUiState.Warning -> {
            ResultScreen(
                scanResult = state.scanResult,
                capturedImage = state.capturedImage,
                onScanAgain = { viewModel.resetState() },
                warning = state.warning,
                dailyEnergyKcal = dailyTargets.energyKcal,
                onPortionChanged = { viewModel.updateConsumedPortion(state.scanResult.scanId, it) },
                onMealTypeChanged = { viewModel.updateMealType(state.scanResult.scanId, it) },
                onCorrect = { viewModel.startCorrection() }
            )
        }
        is ScanUiState.NutritionUnavailable -> {
            NutritionUnavailableScreen(
                scanResult = state.scanResult,
                capturedImage = state.capturedImage,
                message = state.message,
                canRetry = state.canRetry,
                onRetry = { viewModel.retryScan() },
                onScanAgain = { viewModel.resetState() },
                onCorrect = { viewModel.startCorrection() }
            )
        }
        is ScanUiState.Editing -> {
            CorrectionScreen(
                original = state.original,
                isSubmitting = state.isSubmitting,
                errorMessage = state.errorMessage,
                estimateKcal = state.estimateKcal,
                isEstimating = state.isEstimating,
                estimateUnavailable = state.estimateUnavailable,
                onCorrectionChanged = { viewModel.previewCorrection(it) },
                onSubmit = { viewModel.submitCorrection(it) },
                onCancel = { viewModel.cancelCorrection() }
            )
        }
        is ScanUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenHorizontal),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analisis belum berhasil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                if (state.canRetry) {
                    PrimaryButton(
                        text = "Coba Lagi",
                        onClick = { viewModel.retryScan() },
                        icon = Icons.Filled.Refresh
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(
                        text = "Kembali ke Beranda",
                        onClick = { viewModel.resetState() }
                    )
                } else {
                    PrimaryButton(
                        text = "Kembali ke Beranda",
                        onClick = { viewModel.resetState() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenHorizontal),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Izinkan akses camera",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (permanentlyDenied) {
                "Akses kamera sedang dinonaktifkan. Aktifkan melalui Pengaturan → Izin → Kamera agar Anda bisa memotret makanan."
            } else {
                "Calories memakai kamera untuk memotret makanan Anda, lalu menghitung kalori dan kandungan gizinya. Foto hanya dipakai untuk analisis."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = if (permanentlyDenied) "Buka Pengaturan" else "Izinkan Kamera",
            onClick = onRequest
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    context.startActivity(intent)
}

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: Exception) {
        null
    }
}