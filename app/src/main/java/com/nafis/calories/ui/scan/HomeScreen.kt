package com.nafis.calories.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.utils.NutritionFormatter
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Main home screen providing scan actions and recent history. */
@Composable
fun HomeScreen(
    onCameraClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRealtimeScanClick: () -> Unit,
    historyViewModel: HistoryViewModel? = null,
    onHistoryItemClick: ((ScanHistoryEntity, Bitmap?) -> Unit)? = null,
    dailyTargets: DailyTargets = DailyTargetCalculator.default,
    onSetTargetClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val histories by historyViewModel?.histories?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }
    val recentHistories = histories.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        HomeHeader()

        Spacer(modifier = Modifier.height(Spacing.sectionMedium))

        DailySummaryCard(
            histories = histories,
            targets = dailyTargets,
            onSetTargetClick = onSetTargetClick
        )

        Spacer(modifier = Modifier.height(Spacing.sectionLarge))

        Text(
            text = "Cek Kalori Makanan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
        Text(
            text = "Foto makanan Anda, kalori dan gizinya akan dihitung otomatis.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.componentMedium))

        PrimaryButton(
            text = "Foto Makanan",
            onClick = onCameraClick,
            icon = Icons.Default.CameraAlt
        )

        Spacer(modifier = Modifier.height(Spacing.componentSmall))

        SecondaryButton(
            text = "Pilih dari Galeri",
            onClick = onUploadClick,
            icon = Icons.Default.PhotoLibrary
        )

        Spacer(modifier = Modifier.height(Spacing.componentSmall))

        SecondaryButton(
            text = "Scan Otomatis",
            onClick = onRealtimeScanClick,
            icon = Icons.Default.CenterFocusWeak
        )

        if (recentHistories.isNotEmpty() && historyViewModel != null) {
            Spacer(modifier = Modifier.height(Spacing.sectionLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terakhir Dicatat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${histories.size} tersimpan",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.componentMedium)
            ) {
                items(recentHistories, key = { it.id }) { history ->
                    RecentScanCard(
                        history = history,
                        historyViewModel = historyViewModel,
                        onClick = { onHistoryItemClick?.invoke(history, it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.screenBottom))
    }
}

@Composable
private fun HomeHeader() {
    Column {
        Text(
            text = "Calories",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
        Text(
            text = getCurrentDate(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DailySummaryCard(
    histories: List<ScanHistoryEntity>,
    targets: DailyTargets,
    onSetTargetClick: () -> Unit
) {
    val today = histories.filter { isToday(it.timestamp) }
    val kcal = today.sumOf { it.consumedEnergyKcal ?: 0.0 }
    val protein = today.sumOf { it.consumedProteinG ?: 0.0 }
    val carbs = today.sumOf { it.consumedCarbohydrateG ?: 0.0 }
    val fat = today.sumOf { it.consumedFatG ?: 0.0 }
    val remaining = targets.energyKcal - kcal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.componentLarge)) {
            Text(
                text = "Kalori hari ini",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = NutritionFormatter.formatThousands(kcal),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "/ ${NutritionFormatter.formatThousands(targets.energyKcal)} kkal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.componentSmall))
            LinearProgressIndicator(
                progress = { (kcal / targets.energyKcal).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = if (remaining < 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
            Text(
                text = when {
                    today.isEmpty() -> "Belum ada makanan tercatat hari ini"
                    remaining >= 0 -> "Sisa ${NutritionFormatter.formatThousands(remaining)} kkal · ${today.size} makanan tercatat"
                    else -> "Lebih ${NutritionFormatter.formatThousands(-remaining)} kkal dari target · ${today.size} makanan tercatat"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.componentMedium))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.componentMedium)
            ) {
                MacroProgress("Protein", protein, targets.proteinG, ProteinColor, Modifier.weight(1f))
                MacroProgress("Karbohidrat", carbs, targets.carbohydrateG, CarbColor, Modifier.weight(1f))
                MacroProgress("Lemak", fat, targets.fatG, FatColor, Modifier.weight(1f))
            }

            if (!targets.isPersonalized) {
                Spacer(modifier = Modifier.height(Spacing.componentSmall))
                TextButton(
                    onClick = onSetTargetClick,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "Target umum orang dewasa. Atur target pribadi →",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroProgress(
    label: String,
    valueG: Double,
    targetG: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = "${valueG.roundToInt()} / ${targetG.roundToInt()} g",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { (valueG / targetG.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun RecentScanCard(
    history: ScanHistoryEntity,
    historyViewModel: HistoryViewModel,
    onClick: (Bitmap?) -> Unit
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(history.id) {
        scope.launch {
            imageBitmap = historyViewModel.getHistoryImage(history)
        }
    }

    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick(imageBitmap) },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                imageBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = history.mealName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Column(modifier = Modifier.padding(Spacing.componentSmall)) {
                Text(
                    text = history.mealName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = history.consumedEnergyKcal?.roundToInt()?.toString() ?: "-",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "kkal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun getCurrentDate(): String {
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    return dateFormat.format(Date())
}

private fun isToday(timestamp: Long): Boolean {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return dateFormat.format(Date(timestamp)) == dateFormat.format(Date())
}