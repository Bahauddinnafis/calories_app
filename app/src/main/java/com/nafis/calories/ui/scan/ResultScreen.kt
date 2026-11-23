package com.nafis.calories.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.model.ConfidenceLevel
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.Ingredient
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.model.Nutrition
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.ui.components.CalorieDisplay
import com.nafis.calories.ui.components.KeyNutrientsSection
import com.nafis.calories.ui.components.MacroNutrientsRow
import com.nafis.calories.ui.components.MicroNutrientCard
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.NutritionFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ResultScreen(
    scanResult: ScanResult,
    capturedImage: Bitmap?,
    onScanAgain: () -> Unit,
    warning: String? = null,
    primaryActionText: String = "Scan Makanan Lain",
    dailyEnergyKcal: Double = DailyTargetCalculator.DEFAULT_ENERGY_KCAL,
    loggedAt: Long? = null,
    onPortionChanged: ((Double) -> Unit)? = null,
    onMealTypeChanged: ((MealType) -> Unit)? = null,
    onCorrect: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    var currentServingWeightG by remember(scanResult.scanId) {
        mutableDoubleStateOf((scanResult.consumedWeightG ?: scanResult.defaultConsumedWeightG).coerceAtLeast(1.0))
    }
    val updatePortion: (Double) -> Unit = { newWeight ->
        currentServingWeightG = newWeight
        onPortionChanged?.invoke(newWeight)
    }

    val displayResult = remember(scanResult, currentServingWeightG) {
        scaleScanResult(scanResult, currentServingWeightG)
    }

    val displayWeightText = formatGrams(currentServingWeightG)

    val unitName = scanResult.servingUnitName ?: "porsi"
    val unitCount = scanResult.servingUnitCount ?: 1
    val singleWeight = scanResult.singleServingWeightG ?: scanResult.servingWeightG

    val subTitleText = if (scanResult.isMultiUnit) {
        val currentUnits = currentServingWeightG / singleWeight
        val unitText = if (kotlin.math.abs(currentUnits - currentUnits.roundToInt()) < 0.1) {
            "${currentUnits.roundToInt()} $unitName"
        } else {
            String.format(java.util.Locale("id", "ID"), "%.1f %s", currentUnits, unitName)
        }
        "• $unitText · di foto ada $unitCount $unitName"
    } else {
        "• perkiraan dari foto"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        capturedImage?.let { bitmap ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = displayResult.mealName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.sectionSmall))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = displayResult.mealName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayWeightText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subTitleText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (loggedAt != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Dicatat ${formatLoggedAt(loggedAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (scanResult.isUserCorrected) {
                // Backend reports confidence 1.0 here; it reflects the user's input, not the photo.
                Spacer(modifier = Modifier.height(Spacing.componentMedium))
                UserCorrectedBanner()
            } else {
                scanResult.confidenceLevel?.let { level ->
                    Spacer(modifier = Modifier.height(Spacing.componentMedium))
                    ConfidenceBanner(
                        level = level,
                        servingUncertain = scanResult.isServingUncertain,
                        onCorrect = onCorrect
                    )
                }
            }

            if (!warning.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.componentMedium))
                PartialResultBanner(warning)
            }

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            CalorieDisplay(
                calories = displayResult.totalNutrition.energyKcal,
                dailyEnergyKcal = dailyEnergyKcal
            )

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            MacroNutrientsRow(
                protein = displayResult.totalNutrition.proteinG,
                carbs = displayResult.totalNutrition.carbohydrateG,
                fat = displayResult.totalNutrition.fatG
            )

            val missingLabels = NutritionFormatter.missingNutrientLabels(displayResult.totalNutrition)
            if (missingLabels.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.componentSmall))
                Text(
                    text = "Belum terhitung: ${missingLabels.joinToString(", ")}. " +
                        "Bagian ini tidak dihitung, bukan berarti nilainya nol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            KeyNutrientsSection(
                nutrition = displayResult.totalNutrition
            )

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            PortionAdjusterCard(
                originalTotalWeightG = scanResult.servingWeightG,
                singleServingWeightG = scanResult.singleServingWeightG,
                servingUnitName = scanResult.servingUnitName,
                servingUnitCount = scanResult.servingUnitCount,
                currentWeightG = currentServingWeightG,
                highlight = scanResult.isServingUncertain,
                savesToHistory = onPortionChanged != null,
                onWeightChanged = updatePortion
            )

            if (onMealTypeChanged != null) {
                Spacer(modifier = Modifier.height(Spacing.componentMedium))
                MealTypeSelector(
                    selected = scanResult.mealType,
                    onSelected = onMealTypeChanged
                )
            }

            if (displayResult.displayAiNotice && !displayResult.aiNoticeText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.componentMedium))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(Spacing.componentMedium)) {
                        Text(
                            text = "Tentang perkiraan ini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = displayResult.aiNoticeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (displayResult.uncertaintyNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.componentMedium))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(Spacing.componentMedium)) {
                        Text(
                            text = "Catatan",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        displayResult.uncertaintyNotes.forEach { note ->
                            Text(
                                text = "• $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sectionMedium))

            val micronutrients = listOfNotNull(
                displayResult.totalNutrition.fiberG?.let { MicroNutrient("Serat", it, "g", 30.0) },
                displayResult.totalNutrition.calciumMg?.let { MicroNutrient("Kalsium", it, "mg", 1100.0) },
                displayResult.totalNutrition.ironMg?.let { MicroNutrient("Zat besi", it, "mg", 18.0) }
            )

            if (micronutrients.isNotEmpty()) {
                Text(
                    text = "Serat & Mineral",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Persentase dari kebutuhan harian orang dewasa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Spacing.componentSmall))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
                ) {
                    micronutrients.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
                        ) {
                            rowItems.forEach { item ->
                                MicroNutrientCard(
                                    label = item.label,
                                    value = item.value,
                                    unit = item.unit,
                                    dailyTarget = item.dailyTarget,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Keep a lone card at half width so the grid stays aligned.
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sectionMedium))
            }

            if (displayResult.ingredients.isNotEmpty()) {
                Text(
                    text = "Rincian Bahan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(Spacing.componentSmall))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
                ) {
                    displayResult.ingredients.forEach { ingredient ->
                        IngredientItem(ingredient)
                        if (ingredient != displayResult.ingredients.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = Spacing.componentSmall),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sectionMedium))
            }

            Text(
                text = "Nilai gizi di atas adalah perkiraan dari foto dan data referensi gizi. Gunakan sebagai panduan, bukan pengganti saran ahli gizi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.componentSmall)
            )

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            if (onCorrect != null) {
                SecondaryButton(
                    text = "Koreksi Makanan",
                    onClick = onCorrect,
                    icon = Icons.Outlined.Edit
                )
                Spacer(modifier = Modifier.height(Spacing.componentSmall))
            }

            PrimaryButton(
                text = primaryActionText,
                onClick = onScanAgain
            )

            Spacer(modifier = Modifier.height(Spacing.screenBottom))
        }
    }
}

@Composable
private fun PartialResultBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(Spacing.componentMedium),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.componentSmall))
            Column {
                Text(
                    text = "Data gizi belum lengkap",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserCorrectedBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.componentMedium, vertical = Spacing.componentSmall),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.componentSmall))
            Column {
                Text(
                    text = "Dikoreksi oleh Anda",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gizi dihitung ulang dari nama dan berat bahan yang Anda isi.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConfidenceBanner(
    level: ConfidenceLevel,
    servingUncertain: Boolean,
    onCorrect: (() -> Unit)?
) {
    val (title, body) = when (level) {
        ConfidenceLevel.HIGH -> "Perkiraan cukup akurat" to
            "Makanan dan porsinya terlihat jelas di foto."
        ConfidenceLevel.MEDIUM -> "Perkiraan cukup baik" to
            "Sebaiknya cek kembali nama makanan dan porsinya."
        ConfidenceLevel.LOW -> "Perkiraan kasar" to
            "Foto kurang jelas atau makanannya sulit dikenali. Periksa porsinya, atau scan ulang dengan foto yang lebih jelas."
    }
    val text = if (servingUncertain && level == ConfidenceLevel.HIGH) {
        "Makanannya terlihat jelas, tetapi berat porsinya sulit diperkirakan."
    } else {
        body
    }
    val (container, accent) = when (level) {
        ConfidenceLevel.HIGH -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) to MaterialTheme.colorScheme.primary
        ConfidenceLevel.MEDIUM -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) to MaterialTheme.colorScheme.onSurfaceVariant
        ConfidenceLevel.LOW -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f) to MaterialTheme.colorScheme.tertiary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.componentMedium, vertical = Spacing.componentSmall),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (level == ConfidenceLevel.HIGH) Icons.Outlined.Verified else Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.componentSmall))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onCorrect != null && level != ConfidenceLevel.HIGH) {
                    Text(
                        text = "Koreksi makanan →",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = Spacing.componentExtraTiny)
                            .clickable(onClick = onCorrect)
                    )
                }
            }
        }
    }
}

@Composable
private fun MealTypeSelector(selected: MealType?, onSelected: (MealType) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Waktu makan",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.componentSmall))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MealType.entries.forEach { type ->
                val isSelected = type == selected
                Surface(
                    onClick = { onSelected(type) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = type.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatLoggedAt(timestamp: Long): String =
    SimpleDateFormat("EEEE, d MMM yyyy '·' HH.mm", Locale("id", "ID")).format(Date(timestamp))

@Composable
private fun PortionAdjusterCard(
    originalTotalWeightG: Double,
    singleServingWeightG: Double?,
    servingUnitName: String?,
    servingUnitCount: Int?,
    currentWeightG: Double,
    highlight: Boolean,
    savesToHistory: Boolean,
    onWeightChanged: (Double) -> Unit
) {
    val isMultiUnit = (servingUnitCount != null && servingUnitCount > 1 && singleServingWeightG != null && singleServingWeightG > 0)
    val base = singleServingWeightG?.takeIf { isMultiUnit } ?: if (originalTotalWeightG > 0) originalTotalWeightG else 100.0
    val unitName = servingUnitName ?: "porsi"

    val minWeight = (base * 0.1).coerceAtLeast(1.0).toFloat()
    val maxWeight = maxOf(originalTotalWeightG * 2.0, base * 4.0).toFloat()

    val stepG = when {
        base <= 10.0 -> 1.0
        base <= 50.0 -> 5.0
        else -> 25.0
    }

    val currentWeightText = formatGrams(currentWeightG)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Draw attention here when the portion weight is the weakest part of the estimate.
        border = if (highlight) BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sesuaikan Porsi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            highlight -> "Berat porsi sulit diperkirakan dari foto. Pastikan sesuai yang dimakan."
                            savesToHistory -> "Ubah jika porsi yang dimakan berbeda. Tersimpan otomatis."
                            else -> "Ubah jika porsi yang dimakan berbeda"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highlight) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        onClick = { onWeightChanged((currentWeightG - stepG).coerceAtLeast(minWeight.toDouble())) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Kurangi Porsi",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = currentWeightText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )

                    Surface(
                        onClick = { onWeightChanged((currentWeightG + stepG).coerceAtMost(maxWeight.toDouble())) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Tambah Porsi",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = currentWeightG.toFloat().coerceIn(minWeight, maxWeight),
                onValueChange = { onWeightChanged(it.toDouble()) },
                valueRange = minWeight..maxWeight,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val presets: List<Pair<String, Double>> = if (isMultiUnit) {
                    listOf(1, 2, servingUnitCount, servingUnitCount + 1)
                        .distinct()
                        .map { units -> "$units $unitName" to base * units }
                } else {
                    listOf(
                        "½ porsi" to 0.5,
                        "1 porsi" to 1.0,
                        "1½ porsi" to 1.5,
                        "2 porsi" to 2.0
                    ).map { (label, multiplier) -> label to base * multiplier }
                }

                presets.forEach { (label, presetWeight) ->
                    PortionPresetChip(
                        label = label,
                        weightText = formatGrams(presetWeight),
                        selected = kotlin.math.abs(currentWeightG - presetWeight) < (stepG * 0.5),
                        onClick = { onWeightChanged(presetWeight) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IngredientItem(ingredient: Ingredient) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.componentExtraTiny)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ingredient.foodName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatGrams(ingredient.estimatedWeightG),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ingredient.nutrition?.let { nutrition ->
                Text(
                    text = buildString {
                        nutrition.energyKcal?.let {
                            append("${NutritionFormatter.formatEnergy(it)} kkal")
                        }
                        nutrition.proteinG?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${NutritionFormatter.formatMacro(it)} g protein")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val sourceObj = ingredient.source
            val (sourceText, isUnavailable) = when {
                // Without nutrition there is no source to credit, AI or otherwise.
                !ingredient.hasNutrition -> "Data gizi belum tersedia" to true
                ingredient.nutritionSourceType == "ai_estimation" ||
                    ingredient.matchStatus == "estimated" ||
                    sourceObj?.sourceName == "AI nutrition estimate" ||
                    sourceObj == null -> "Perkiraan AI" to false
                else -> "Data ${sourceObj.sourceName} ${sourceObj.sourceVersion}".trim() to false
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isUnavailable) {
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                }
            ) {
                Text(
                    text = sourceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUnavailable) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun PortionPresetChip(
    label: String,
    weightText: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = weightText,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f),
                maxLines = 1
            )
        }
    }
}

private data class MicroNutrient(
    val label: String,
    val value: Double,
    val unit: String,
    val dailyTarget: Double
)

/** Formats grams as "25 g" or "2,5 g" (Indonesian decimal comma). */
private fun formatGrams(value: Double): String {
    return if (kotlin.math.abs(value - value.roundToInt()) < 0.1) {
        "${value.roundToInt()} g"
    } else {
        String.format(java.util.Locale("id", "ID"), "%.1f g", value)
    }
}

private fun scaleScanResult(original: ScanResult, newWeightG: Double): ScanResult {
    val baseWeight = if (original.servingWeightG > 0) original.servingWeightG else 1.0
    val scaleFactor = newWeightG / baseWeight

    val scaledTotalNutrition = Nutrition(
        energyKcal = original.totalNutrition.energyKcal?.let { it * scaleFactor },
        proteinG = original.totalNutrition.proteinG?.let { it * scaleFactor },
        fatG = original.totalNutrition.fatG?.let { it * scaleFactor },
        carbohydrateG = original.totalNutrition.carbohydrateG?.let { it * scaleFactor },
        sugarG = original.totalNutrition.sugarG?.let { it * scaleFactor },
        saturatedFatG = original.totalNutrition.saturatedFatG?.let { it * scaleFactor },
        cholesterolMg = original.totalNutrition.cholesterolMg?.let { it * scaleFactor },
        fiberG = original.totalNutrition.fiberG?.let { it * scaleFactor },
        sodiumMg = original.totalNutrition.sodiumMg?.let { it * scaleFactor },
        calciumMg = original.totalNutrition.calciumMg?.let { it * scaleFactor },
        ironMg = original.totalNutrition.ironMg?.let { it * scaleFactor }
    )

    val scaledIngredients = original.ingredients.map { ing ->
        ing.copy(
            estimatedWeightG = ing.estimatedWeightG * scaleFactor,
            nutrition = ing.nutrition?.let { n ->
                Nutrition(
                    energyKcal = n.energyKcal?.let { it * scaleFactor },
                    proteinG = n.proteinG?.let { it * scaleFactor },
                    fatG = n.fatG?.let { it * scaleFactor },
                    carbohydrateG = n.carbohydrateG?.let { it * scaleFactor },
                    sugarG = n.sugarG?.let { it * scaleFactor },
                    saturatedFatG = n.saturatedFatG?.let { it * scaleFactor },
                    cholesterolMg = n.cholesterolMg?.let { it * scaleFactor },
                    fiberG = n.fiberG?.let { it * scaleFactor },
                    sodiumMg = n.sodiumMg?.let { it * scaleFactor },
                    calciumMg = n.calciumMg?.let { it * scaleFactor },
                    ironMg = n.ironMg?.let { it * scaleFactor }
                )
            }
        )
    }

    return original.copy(
        servingWeightG = newWeightG,
        totalNutrition = scaledTotalNutrition,
        ingredients = scaledIngredients
    )
}