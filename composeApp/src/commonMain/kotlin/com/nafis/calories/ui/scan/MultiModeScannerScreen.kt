package com.nafis.calories.ui.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nafis.calories.ui.platform.CameraController
import com.nafis.calories.ui.platform.CameraPreview
import com.nafis.calories.ui.platform.rememberCameraController
import com.nafis.calories.ui.platform.rememberGalleryPicker
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.PrimaryDark
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

enum class ScanMode {
    REALTIME,
    CAMERA,
    GALLERY
}

private val ScreenBackground = Color(0xFF0B0F17)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate400 = Color(0xFF94A3B8)
private const val HOLD_DURATION_MS = 1_500L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiModeScannerScreen(
    onImageReady: (ByteArray) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val camera = rememberCameraController()
    val openGallery = rememberGalleryPicker(onImageReady)

    var activeMode by remember { mutableStateOf(ScanMode.REALTIME) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showGallerySheet by remember { mutableStateOf(false) }

    fun capture(onDone: () -> Unit = {}) {
        isProcessing = true
        scope.captureAndProcess(camera, onImageReady) {
            isProcessing = false
            onDone()
        }
    }

    LaunchedEffect(isHolding) {
        holdProgress = 0f
        if (!isHolding) return@LaunchedEffect
        val start = TimeSource.Monotonic.markNow()
        while (holdProgress < 1f) {
            holdProgress = (start.elapsedNow().inWholeMilliseconds.toFloat() / HOLD_DURATION_MS).coerceIn(0f, 1f)
            delay(16)
        }
        capture { isHolding = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            ScannerTopBar(activeMode = activeMode, onBack = onBack)

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Slate800),
                contentAlignment = Alignment.Center
            ) {
                CameraPreview(controller = camera, modifier = Modifier.fillMaxSize())
                ViewfinderBracketsOverlay()
                HoldReticle(holdProgress = holdProgress, isHolding = isHolding, activeMode = activeMode)
                DetectionCard(activeMode = activeMode)
                if (isProcessing) ProcessingOverlay()
            }

            Spacer(modifier = Modifier.height(14.dp))

            ModeSwitcher(
                activeMode = activeMode,
                onModeSelected = { mode ->
                    activeMode = mode
                    if (mode == ScanMode.GALLERY) showGallerySheet = true
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        activeMode = ScanMode.GALLERY
                        showGallerySheet = true
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .background(Slate800, RoundedCornerShape(14.dp))
                        .border(1.dp, Slate700, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Upload Galeri",
                        tint = Color(0xFFF8FAFC)
                    )
                }

                ShutterButton(
                    label = when (activeMode) {
                        ScanMode.REALTIME -> if (isHolding) "CANCEL" else "HOLD"
                        ScanMode.CAMERA -> "FOTO"
                        ScanMode.GALLERY -> "PILIH"
                    },
                    onClick = {
                        when (activeMode) {
                            ScanMode.REALTIME -> isHolding = !isHolding
                            ScanMode.CAMERA -> capture()
                            ScanMode.GALLERY -> openGallery()
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Slate800)
                        .border(1.dp, Slate700, RoundedCornerShape(14.dp))
                        .clickable { isHolding = true },
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

        if (showGallerySheet) {
            ModalBottomSheet(
                onDismissRequest = { showGallerySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface,
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
                GallerySheetContent(
                    onClose = { showGallerySheet = false },
                    onOpenGallery = {
                        showGallerySheet = false
                        openGallery()
                    }
                )
            }
        }
    }
}

@Composable
private fun ScannerTopBar(activeMode: ScanMode, onBack: () -> Unit) {
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
            Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
        }

        Surface(
            shape = CircleShape,
            color = Color(0xFF0D9488).copy(alpha = 0.2f),
            border = BorderStroke(1.dp, PrimaryDark.copy(alpha = 0.4f))
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
}

@Composable
private fun HoldReticle(holdProgress: Float, isHolding: Boolean, activeMode: ScanMode) {
    Box(modifier = Modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = size.minDimension / 2 - 4.dp.toPx(),
                style = Stroke(width = 4.dp.toPx())
            )
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

        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(CircleShape)
                .background(Slate800.copy(alpha = 0.85f))
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
                    text = when {
                        isHolding -> "Menahan... ${(holdProgress * 100).toInt()}%"
                        activeMode == ScanMode.REALTIME -> "Tahan Perangkat"
                        else -> "Siap Memotret"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDark,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun BoxScope.DetectionCard(activeMode: ScanMode) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
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
                color = Slate400,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun ProcessingOverlay() {
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

@Composable
private fun ModeSwitcher(activeMode: ScanMode, onModeSelected: (ScanMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF131926), shape = RoundedCornerShape(14.dp))
            .border(1.dp, Slate800, RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ModeTab("Langsung", activeMode == ScanMode.REALTIME) { onModeSelected(ScanMode.REALTIME) }
        ModeTab("Foto", activeMode == ScanMode.CAMERA) { onModeSelected(ScanMode.CAMERA) }
        ModeTab("Galeri", activeMode == ScanMode.GALLERY) { onModeSelected(ScanMode.GALLERY) }
    }
}

@Composable
private fun RowScope.ModeTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Slate400
        )
    }
}

@Composable
private fun ShutterButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(66.dp)
            .clip(CircleShape)
            .border(3.dp, PrimaryDark, CircleShape)
            .padding(4.dp)
            .clickable(onClick = onClick),
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
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun GallerySheetContent(onClose: () -> Unit, onOpenGallery: () -> Unit) {
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
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Tutup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenGallery),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
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

@Composable
private fun ViewfinderBracketsOverlay() {
    Box(modifier = Modifier.fillMaxSize()) {
        Bracket(Alignment.TopStart, RoundedCornerShape(topStart = 6.dp))
        Bracket(Alignment.TopEnd, RoundedCornerShape(topEnd = 6.dp))
        Bracket(Alignment.BottomStart, RoundedCornerShape(bottomStart = 6.dp))
        Bracket(Alignment.BottomEnd, RoundedCornerShape(bottomEnd = 6.dp))
    }
}

@Composable
private fun BoxScope.Bracket(alignment: Alignment, shape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .padding(20.dp)
            .size(26.dp)
            .align(alignment)
            .border(width = 3.dp, color = PrimaryDark, shape = shape)
    )
}

private fun CoroutineScope.captureAndProcess(
    camera: CameraController,
    onImageReady: (ByteArray) -> Unit,
    onComplete: () -> Unit
) {
    launch {
        camera.capture()?.let(onImageReady)
        onComplete()
    }
}