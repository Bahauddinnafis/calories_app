package com.nafis.calories.ui.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.model.formatKcalInt
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.components.rememberImageBitmap
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

private val MANUAL_ENTRY_PREFIXES = listOf("manual_", "rec_", "product_")

@Composable
fun ResultScreen(
    scanResult: ScanResult,
    capturedImage: ByteArray?,
    onScanAgain: () -> Unit,
    primaryActionText: String = "Simpan ke Catatan",
    onPortionChanged: ((Double) -> Unit)? = null,
    onCorrect: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    var currentServingWeightG by remember(scanResult.scanId) {
        mutableDoubleStateOf(scanResult.effectiveConsumedWeightG.coerceAtLeast(1.0))
    }

    val updatePortion: (Double) -> Unit = { newWeight ->
        currentServingWeightG = newWeight
        onPortionChanged?.invoke(newWeight)
    }

    val baseFullWeight = scanResult.servingWeightG.takeIf { it > 0.0 } ?: scanResult.defaultConsumedWeightG
    val ratio = (currentServingWeightG / baseFullWeight).coerceIn(0.05, 5.0)

    val totalPhotoKcal = scanResult.totalNutrition.energyKcal.formatKcalInt()
    val onePieceKcal = ((scanResult.totalNutrition.energyKcal ?: 0.0) * (scanResult.defaultConsumedWeightG / baseFullWeight)).formatKcalInt()
    val selectedConsumedKcal = ((scanResult.totalNutrition.energyKcal ?: 0.0) * ratio).formatKcalInt()

    val displayResult = remember(scanResult, currentServingWeightG) {
        scaleScanResult(scanResult, currentServingWeightG)
    }

    val unitName = scanResult.servingUnitName?.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    } ?: "Porsi"

    fun formatMacro(value: Double?): String {
        return if (value == null) "-" else "${value.roundToInt()}g"
    }

    val carbText = formatMacro(displayResult.totalNutrition.carbohydrateG)
    val protText = formatMacro(displayResult.totalNutrition.proteinG)
    val fatText = formatMacro(displayResult.totalNutrition.fatG)

    val confidencePercent = scanResult.overallConfidence
        .takeIf { !it.isNaN() && it > 0.0 }
        ?.let { (it.coerceAtMost(1.0) * 100).roundToInt() }

    val displayImage = rememberImageBitmap(capturedImage)?.takeIf { it.width > 1 && it.height > 1 }
    val isManualEntry = MANUAL_ENTRY_PREFIXES.any { scanResult.scanId.startsWith(it) }
    val personalNote = scanResult.personalNutritionNote?.trim()?.takeIf { it.isNotEmpty() }
    val aiDisclaimer = scanResult.aiDisclaimerText?.trim()?.takeIf { it.isNotEmpty() && !isManualEntry }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = if (isManualEntry) "Detail Catatan" else "Hasil Analisis Cloud AI",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(38.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        displayImage?.let { bitmap ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = scanResult.mealName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val headerLabel = when {
                            isManualEntry -> "Dicatat tanpa foto"
                            confidencePercent != null -> "Akurasi Deteksi $confidencePercent%"
                            else -> null
                        }
                        if (headerLabel != null) {
                            Text(
                                text = headerLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5EEAD4)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Text(
                            text = scanResult.mealName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = selectedConsumedKcal.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF5EEAD4)
                        )
                        Text(
                            text = "Yang Anda Makan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Estimasi 1 $unitName",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF5EEAD4),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF1E293B)))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Karbohidrat", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(carbText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Column {
                        Text("Protein", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(protText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF5EEAD4))
                    }
                    Column {
                        Text("Lemak", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        Text(fatText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1E293B)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Per 1 $unitName",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "~$onePieceKcal kkal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5EEAD4)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "Total Seluruh Foto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$totalPhotoKcal kkal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF334155)))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "AI mendeteksi total $totalPhotoKcal kkal untuk seluruh makanan di foto ini. Pilih jumlah yang Anda makan di bawah agar catatan harian tetap akurat.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Berapa banyak yang Anda makan?",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isPortion1 = abs(currentServingWeightG - scanResult.defaultConsumedWeightG) < 5.0
            val isPortion2 = abs(currentServingWeightG - (scanResult.defaultConsumedWeightG * 2)) < 5.0
            val isHalfPlate = abs(currentServingWeightG - (baseFullWeight * 0.5)) < 5.0
            val isFullPhoto = !isPortion1 && !isPortion2 && !isHalfPlate

            PortionChip(
                label = "1 $unitName\n(~$onePieceKcal kkal)",
                isSelected = isPortion1,
                onClick = { updatePortion(scanResult.defaultConsumedWeightG) },
                modifier = Modifier.weight(1f)
            )
            PortionChip(
                label = "2 $unitName\n(~${onePieceKcal * 2} kkal)",
                isSelected = isPortion2,
                onClick = { updatePortion(scanResult.defaultConsumedWeightG * 2) },
                modifier = Modifier.weight(1f)
            )
            PortionChip(
                label = "½ Foto\n(~${totalPhotoKcal / 2} kkal)",
                isSelected = isHalfPlate,
                onClick = { updatePortion(baseFullWeight * 0.5) },
                modifier = Modifier.weight(1f)
            )
            PortionChip(
                label = "Semua Foto\n($totalPhotoKcal kkal)",
                isSelected = isFullPhoto,
                onClick = { updatePortion(baseFullWeight) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Komponen Bahan Terdeteksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Rincian Bahan (Total Foto)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (displayResult.ingredients.isEmpty()) {
                    Text(
                        text = if (isManualEntry) "Catatan ini tidak memiliki rincian bahan." else "Rincian bahan belum tersedia untuk hasil ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    displayResult.ingredients.forEachIndexed { index, ing ->
                        val sourceName = ing.source?.sourceName
                        val sourceLabel = when {
                            !sourceName.isNullOrBlank() -> "Sumber: $sourceName"
                            ing.nutritionSourceType.equals("ai", ignoreCase = true) -> "Sumber: Estimasi AI"
                            !ing.nutritionSourceType.isNullOrBlank() -> "Sumber: ${ing.nutritionSourceType}"
                            else -> null
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ing.foodName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${ing.nutrition?.energyKcal.formatKcalInt()} kkal",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (sourceLabel != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = sourceLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (index < displayResult.ingredients.size - 1) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Data nilai gizi merujuk pada basis data pangan resmi (TKPI/USDA) jika tersedia, atau estimasi model AI untuk hidangan campuran.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        if (aiDisclaimer != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = aiDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (personalNote != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = ProteinBgColor,
                border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Catat Gizi",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = personalNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (onCorrect != null) {
                SecondaryButton(
                    text = "Koreksi",
                    onClick = onCorrect,
                    icon = Icons.Outlined.Edit,
                    modifier = Modifier.weight(1f)
                )
            }

            PrimaryButton(
                text = primaryActionText,
                onClick = onScanAgain,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(104.dp))
    }
}

@Composable
private fun PortionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) ProteinBgColor else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (isSelected) Primary else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun scaleScanResult(original: ScanResult, servingWeightG: Double): ScanResult {
    val baseWeight = original.servingWeightG.takeIf { it > 0.0 } ?: original.defaultConsumedWeightG
    val ratio = (servingWeightG / baseWeight).coerceIn(0.05, 5.0)

    return original.copy(
        consumedWeightG = servingWeightG,
        totalNutrition = original.totalNutrition.scaled(ratio),
        ingredients = original.ingredients.map { ing ->
            ing.copy(
                estimatedWeightG = ing.estimatedWeightG * ratio,
                nutrition = ing.nutrition?.scaled(ratio)
            )
        }
    )
}