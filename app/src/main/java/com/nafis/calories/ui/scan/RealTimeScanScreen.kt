package com.nafis.calories.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.ml.FoodDetector
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.CameraUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeScanScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var detectedFood by remember { mutableStateOf<String?>(null) }
    var consecutiveDetections by remember { mutableStateOf(0) }
    var lastDetectedFood by remember { mutableStateOf<String?>(null) }
    var isInCooldown by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var capturedImageForSheet by remember { mutableStateOf<Bitmap?>(null) }
    var showResultSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    var resultWarning by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun showTemporaryError(message: String) {
        errorMessage = message
        isScanning = false
        isInCooldown = true
        delay(3000)
        isInCooldown = false
        errorMessage = null
        consecutiveDetections = 0
        lastDetectedFood = null
        viewModel.navigateToRealTimeScan()
    }

    LaunchedEffect(uiState) {
        // Every terminal scan state must end the "analysing" overlay, otherwise the screen hangs.
        val (result, image, warning) = when (val state = uiState) {
            is ScanUiState.Success -> Triple(state.scanResult, state.capturedImage, null)
            is ScanUiState.Warning -> Triple(state.scanResult, state.capturedImage, state.warning)
            is ScanUiState.NutritionUnavailable -> {
                showTemporaryError("${state.scanResult.mealName} dikenali, tetapi kalorinya belum bisa dihitung. Coba arahkan ulang kamera.")
                return@LaunchedEffect
            }
            is ScanUiState.Error -> {
                showTemporaryError(state.message)
                return@LaunchedEffect
            }
            else -> return@LaunchedEffect
        }
        scanResult = result
        capturedImageForSheet = image
        resultWarning = warning
        showResultSheet = true
        isScanning = false
        errorMessage = null
        isInCooldown = true
        delay(5000)
        isInCooldown = false
        consecutiveDetections = 0
        lastDetectedFood = null
        viewModel.navigateToRealTimeScan()
    }

    val foodDetector = remember { FoodDetector(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            if (!isScanning && !isInCooldown && !showResultSheet) {
                scope.launch {
                    captureFrameForDetection(
                        context = context,
                        imageCapture = imageCapture,
                        foodDetector = foodDetector,
                        onDetection = { label, confidence ->
                            if (confidence > 0.4f) {
                                detectedFood = label
                                if (label == lastDetectedFood) {
                                    consecutiveDetections++
                                    if (consecutiveDetections >= 3 && !isScanning) {
                                        isScanning = true
                                        captureFrame(
                                            context = context,
                                            imageCapture = imageCapture,
                                            onImageCaptured = { bitmap ->
                                                viewModel.scanFoodRealTime(bitmap)
                                            },
                                            onError = {
                                                isScanning = false
                                                consecutiveDetections = 0
                                            }
                                        )
                                    }
                                } else {
                                    lastDetectedFood = label
                                    consecutiveDetections = 1
                                }
                            } else {
                                detectedFood = null
                                lastDetectedFood = null
                                consecutiveDetections = 0
                            }
                        }
                    )
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            foodDetector.close()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.componentMedium)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Menghitung kalori…",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.Center)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.large
                )
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.componentMedium)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (!showResultSheet && errorMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = Spacing.sectionMedium)
                    .padding(horizontal = Spacing.screenHorizontal),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            ) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = Spacing.componentLarge, vertical = Spacing.componentMedium)
                )
            }
        }

        if (!isScanning && !showResultSheet && !isInCooldown && errorMessage == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Spacing.sectionLarge)
                    .padding(horizontal = Spacing.screenHorizontal),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Text(
                    text = if (detectedFood != null) {
                        "Terdeteksi $detectedFood, tahan sebentar…"
                    } else {
                        "Arahkan kamera ke makanan. Scan berjalan otomatis."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Spacing.componentLarge, vertical = Spacing.componentMedium)
                )
            }
        }
    }

    if (showResultSheet && scanResult != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showResultSheet = false
                scanResult = null
                capturedImageForSheet = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ResultScreen(
                scanResult = scanResult!!,
                capturedImage = capturedImageForSheet,
                warning = resultWarning,
                primaryActionText = "Lanjut Scan",
                onScanAgain = {
                    showResultSheet = false
                    scanResult = null
                    capturedImageForSheet = null
                }
            )
        }
    }
}

private suspend fun captureFrameForDetection(
    context: Context,
    imageCapture: ImageCapture,
    foodDetector: FoodDetector,
    onDetection: (String, Float) -> Unit
) {
    try {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val bitmap = CameraUtils.imageProxyToBitmap(image)
                        val rotatedBitmap = CameraUtils.rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())

                        CoroutineScope(Dispatchers.Default).launch {
                            val result = foodDetector.detectFood(rotatedBitmap)
                            result.getOrNull()?.let { detection ->
                                onDetection(detection.label, detection.score)
                            }
                        }
                    } catch (e: Exception) {
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                }
            }
        )
    } catch (e: Exception) {
    }
}

private fun captureFrame(
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit,
    onError: (String) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = CameraUtils.imageProxyToBitmap(image)
                    val rotatedBitmap = CameraUtils.rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
                    onImageCaptured(rotatedBitmap)
                } catch (e: Exception) {
                    onError("Gagal memproses gambar: ${e.message}")
                } finally {
                    image.close()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception.message ?: "Pengambilan foto gagal")
            }
        }
    )
}