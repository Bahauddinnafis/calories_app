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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.ui.history.HistoryScreen
import com.nafis.calories.ui.profile.ProfileScreen
import com.nafis.calories.ui.profile.ProfileViewModel
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.scan.CameraScreen
import com.nafis.calories.ui.scan.CorrectionScreen
import com.nafis.calories.ui.scan.HomeScreen
import com.nafis.calories.ui.scan.NutritionUnavailableScreen
import com.nafis.calories.ui.scan.RealTimeScanScreen
import com.nafis.calories.ui.scan.ResultScreen
import com.nafis.calories.ui.scan.ScanUiState
import com.nafis.calories.ui.scan.ScanViewModel
import com.nafis.calories.ui.theme.CaloriesTheme
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

                    if (cameraPermissionState.status.isGranted) {
                        MainApp(scanViewModel, historyViewModel, profileViewModel)
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
    object Profile : Screen("profile", "Profil", Icons.Filled.Person)
}

@Composable
fun MainApp(
    scanViewModel: ScanViewModel,
    historyViewModel: HistoryViewModel,
    profileViewModel: ProfileViewModel
) {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scanUiState by scanViewModel.uiState.collectAsState()
    val dailyTargets by profileViewModel.dailyTargets.collectAsState()

    val isMainHome = (selectedScreen == Screen.Home) && (scanUiState is ScanUiState.Home)
    BackHandler(enabled = !isMainHome) {
        if (scanUiState is ScanUiState.Editing) {
            // Back from the correction form returns to the result, not all the way home.
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
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Screen.Home.icon, contentDescription = null) },
                        label = { Text(Screen.Home.label) },
                        selected = selectedScreen == Screen.Home,
                        onClick = {
                            selectedScreen = Screen.Home
                            if (scanUiState !is ScanUiState.Home) {
                                scanViewModel.resetState()
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Screen.History.icon, contentDescription = null) },
                        label = { Text(Screen.History.label) },
                        selected = selectedScreen == Screen.History,
                        onClick = { selectedScreen = Screen.History }
                    )
                    NavigationBarItem(
                        icon = { Icon(Screen.Profile.icon, contentDescription = null) },
                        label = { Text(Screen.Profile.label) },
                        selected = selectedScreen == Screen.Profile,
                        onClick = { selectedScreen = Screen.Profile }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
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
                Screen.Profile -> ProfileScreen(
                    viewModel = profileViewModel,
                    onSaved = {
                        selectedScreen = Screen.Home
                        if (scanUiState !is ScanUiState.Home) scanViewModel.resetState()
                    }
                )
            }
        }
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
                onSetTargetClick = onOpenProfile
            )
        }
        is ScanUiState.Camera -> {
            CameraScreen(
                onImageCaptured = { bitmap -> viewModel.scanFood(bitmap) },
                onError = { viewModel.resetState() },
                onBack = { viewModel.resetState() }
            )
        }
        is ScanUiState.RealTimeScan -> {
            RealTimeScanScreen(
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
                primaryActionText = if (state.fromHistory) "Tutup" else "Scan Makanan Lain",
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
            text = "Izinkan akses kamera",
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