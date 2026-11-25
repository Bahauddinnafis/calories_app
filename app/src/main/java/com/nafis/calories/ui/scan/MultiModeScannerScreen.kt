package com.nafis.calories.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nafis.calories.ml.FoodDetector
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.PrimaryDark
import com.nafis.calories.utils.CameraUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScanMode {
    REALTIME,
    CAMERA,
    GALLERY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiModeScannerScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var activeMode by remember { mutableStateOf(ScanMode.REALTIME) }

    var detectedFoodLabel by remember { mutableStateOf("Menganalisis objek...") }
    var detectedConfidence by remember { mutableFloatStateOf(0.0f) }
    var holdProgress by remember { mutableFloatStateOf(0f) } // 0f to 1f
    var isHolding by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    var showGallerySheet by remember { mutableStateOf(false) }
    val gallerySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val foodDetector = remember { FoodDetector(context) }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // System gallery picker launcher
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(context, it)
            bitmap?.let { bmp ->
                viewModel.scanFood(bmp)
            }
        }
    }

    // Auto-scan hold animation when holding steady in REALTIME mode
    LaunchedEffect(isHolding) {
        if (isHolding) {
            holdProgress = 0f
            val startTime = System.currentTimeMillis()
            val totalHoldMs = 1500L
            while (isHolding && holdProgress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                holdProgress = (elapsed.toFloat() / totalHoldMs).coerceIn(0f, 1f)
                if (holdProgress >= 1f) {
                    isProcessing = true
                    captureAndProcess(context, imageCapture, viewModel) {
                        isProcessing = false
                        isHolding = false
                    }
                    break
                }
                delay(16)
            }
        } else {
            holdProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F17))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }

                // Status Pill
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D9488).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryDark.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = when (activeMode) {
                                ScanMode.REALTIME -> "Deteksi Langsung • Tahan Kamera"
                                ScanMode.CAMERA -> "Mode Ambil Foto"
                                ScanMode.GALLERY -> "Unggah dari Galeri"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(38.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Viewfinder Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                // Camera Preview surface
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
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Corner Brackets Overlay
                ViewfinderBracketsOverlay()

                // Radial Progress Reticle in Center
                Box(
                    modifier = Modifier.size(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background track circle
                        drawCircle(
                            color = Color.White.copy(alpha = 0.15f),
                            radius = size.minDimension / 2 - 4.dp.toPx(),
                            style = Stroke(width = 4.dp.toPx())
                        )
                        // Hold progress ring
                        if (holdProgress > 0f) {
                            drawArc(
                                color = PrimaryDark,
                                startAngle = -90f,
                                sweepAngle = 360f * holdProgress,
                                useCenter = false,
                                style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Inner Circle Content
                    Box(
                        modifier = Modifier
                            .size(136.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusWeak,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isHolding) {
                                    "Menahan... ${(holdProgress * 100).toInt()}%"
                                } else if (activeMode == ScanMode.REALTIME) {
                                    "Tahan Perangkat"
                                } else {
                                    "Siap Memotret"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDark,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Detection Lock Card at Bottom of Viewfinder
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.9f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp, 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Makanan Terdeteksi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDark,
                            fontSize = 10.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (activeMode == ScanMode.REALTIME) "Paket Makanan Siap Pindai" else "Posisikan Makanan di Bingkai",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tahan perangkat 1,5 detik untuk hasil otomatis",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryDark)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Menganalisis Nutrisi Makanan…",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Switcher Tabs (3 Scan Options)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131926), shape = RoundedCornerShape(14.dp))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeMode == ScanMode.REALTIME) Primary else Color.Transparent)
                        .clickable { activeMode = ScanMode.REALTIME },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Langsung",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeMode == ScanMode.REALTIME) Color.White else Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeMode == ScanMode.CAMERA) Primary else Color.Transparent)
                        .clickable { activeMode = ScanMode.CAMERA },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Foto",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeMode == ScanMode.CAMERA) Color.White else Color(0xFF94A3B8)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeMode == ScanMode.GALLERY) Primary else Color.Transparent)
                        .clickable {
                            activeMode = ScanMode.GALLERY
                            showGallerySheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Galeri",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeMode == ScanMode.GALLERY) Color.White else Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Shortcut Button
                IconButton(
                    onClick = {
                        activeMode = ScanMode.GALLERY
                        showGallerySheet = true
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload Galeri",
                        tint = Color(0xFFF8FAFC)
                    )
                }

                // Main Circular Shutter / Hold Button
                Box(
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .border(3.dp, PrimaryDark, CircleShape)
                        .padding(4.dp)
                        .clickable {
                            if (activeMode == ScanMode.REALTIME) {
                                isHolding = !isHolding
                            } else if (activeMode == ScanMode.CAMERA) {
                                isProcessing = true
                                captureAndProcess(context, imageCapture, viewModel) {
                                    isProcessing = false
                                }
                            } else {
                                galleryPickerLauncher.launch("image/*")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (activeMode) {
                                ScanMode.REALTIME -> if (isHolding) "CANCEL" else "HOLD"
                                ScanMode.CAMERA -> "FOTO"
                                ScanMode.GALLERY -> "PILIH"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }

                // Simulate Auto Hold Trigger Button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                        .clickable {
                            isHolding = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "OTOMATIS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Gallery Bottom Sheet
        if (showGallerySheet) {
            ModalBottomSheet(
                onDismissRequest = { showGallerySheet = false },
                sheetState = gallerySheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Pilih Foto dari Galeri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Foto dari galeri perangkat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showGallerySheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGallerySheet = false
                                galleryPickerLauncher.launch("image/*")
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = Primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = "Buka Galeri",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Pilih foto makanan dari galeri perangkat",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
private fun ViewfinderBracketsOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Left Bracket
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(26.dp)
                .align(Alignment.TopStart)
                .border(
                    width = 3.dp,
                    color = PrimaryDark,
                    shape = RoundedCornerShape(topStart = 6.dp)
                )
        )
        // Top Right Bracket
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(26.dp)
                .align(Alignment.TopEnd)
                .border(
                    width = 3.dp,
                    color = PrimaryDark,
                    shape = RoundedCornerShape(topEnd = 6.dp)
                )
        )
        // Bottom Left Bracket
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(26.dp)
                .align(Alignment.BottomStart)
                .border(
                    width = 3.dp,
                    color = PrimaryDark,
                    shape = RoundedCornerShape(bottomStart = 6.dp)
                )
        )
        // Bottom Right Bracket
        Box(
            modifier = Modifier
                .padding(20.dp)
                .size(26.dp)
                .align(Alignment.BottomEnd)
                .border(
                    width = 3.dp,
                    color = PrimaryDark,
                    shape = RoundedCornerShape(bottomEnd = 6.dp)
                )
        )
    }
}

private fun captureAndProcess(
    context: Context,
    imageCapture: ImageCapture,
    viewModel: ScanViewModel,
    onComplete: () -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val bitmap = CameraUtils.imageProxyToBitmap(image)
                    val rotated = CameraUtils.rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
                    viewModel.scanFood(rotated)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    image.close()
                    onComplete()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
                onComplete()
            }
        }
    )
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