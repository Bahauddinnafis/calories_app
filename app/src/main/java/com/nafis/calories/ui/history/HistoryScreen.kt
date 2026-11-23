package com.nafis.calories.ui.history

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.utils.DailyTotals
import com.nafis.calories.utils.DayTotal
import com.nafis.calories.utils.NutritionFormatter
import com.nafis.calories.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Screen component for viewing and managing scan history records. */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onHistoryItemClick: ((ScanHistoryEntity, Bitmap?) -> Unit)? = null,
    dailyTargetKcal: Double = DailyTargetCalculator.DEFAULT_ENERGY_KCAL
) {
    val histories by viewModel.histories.collectAsState(initial = emptyList())
    // Already sorted newest first by the DAO; groupBy keeps that order.
    val historiesByDay = remember(histories) { histories.groupBy { DailyTotals.startOfDay(it.timestamp) } }
    val weeklyTotals = remember(histories) {
        DailyTotals.lastDays(histories.map { it.timestamp to (it.consumedEnergyKcal ?: 0.0) })
    }
    val historyCount by viewModel.historyCount.collectAsState(initial = 0)
    val deleteState by viewModel.deleteState.collectAsState()

    var selectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ScanHistoryEntity?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.screenTop))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    Text(
                        text = "${selectedItems.size} dipilih",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Column {
                        Text(
                            text = "Riwayat",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (historyCount > 0) {
                            Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
                            Text(
                                text = "$historyCount makanan tersimpan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (historyCount > 0) {
                Spacer(modifier = Modifier.height(Spacing.componentMedium))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
                ) {
                    if (selectionMode) {
                        OutlinedButton(
                            onClick = {
                                selectedItems = if (selectedItems.size == histories.size) emptySet() else histories.map { it.id }.toSet()
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(if (selectedItems.size == histories.size) "Batal Pilih Semua" else "Pilih Semua")
                        }

                        OutlinedButton(
                            onClick = {
                                selectionMode = false
                                selectedItems = emptySet()
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Batal")
                        }
                    } else {
                        // The destructive "delete all" is intentionally less prominent than "select".
                        OutlinedButton(
                            onClick = { selectionMode = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Pilih")
                        }

                        TextButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Hapus Semua", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            if (histories.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
                ) {
                    if (!selectionMode) {
                        item(key = "weekly-trend") {
                            WeeklyTrendCard(totals = weeklyTotals, targetKcal = dailyTargetKcal)
                            Spacer(modifier = Modifier.height(Spacing.componentSmall))
                        }
                    }

                    historiesByDay.forEach { (dayStart, dayItems) ->
                        item(key = "day-$dayStart") {
                            DayHeader(
                                dayStart = dayStart,
                                totalKcal = dayItems.sumOf { it.consumedEnergyKcal ?: 0.0 }
                            )
                        }
                        items(
                            items = dayItems,
                            key = { it.id }
                        ) { history ->
                            HistoryItemCard(
                                history = history,
                                viewModel = viewModel,
                                selectionMode = selectionMode,
                                isSelected = selectedItems.contains(history.id),
                                onSelectionChange = { selected ->
                                    selectedItems = if (selected) selectedItems + history.id else selectedItems - history.id
                                },
                                onDelete = { pendingDelete = history },
                                onClick = onHistoryItemClick
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        if (selectionMode && selectedItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus ${selectedItems.size} Riwayat")
                }
            }
        }

        when (deleteState) {
            is DeleteState.Deleting -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Menghapus riwayat…")
                        }
                    }
                }
            }
            is DeleteState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Riwayat gagal dihapus. Silakan coba lagi.",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { viewModel.clearDeleteState() }) {
                                Text("Tutup")
                            }
                        }
                    }
                }
            }
            else -> {}
        }

        pendingDelete?.let { history ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("Hapus riwayat ini?") },
                text = { Text("\"${history.mealName}\" akan dihapus dari riwayat.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteHistory(history)
                        pendingDelete = null
                    }) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            val scope = rememberCoroutineScope()
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Hapus ${selectedItems.size} riwayat?") },
                text = { Text("Riwayat yang dipilih akan dihapus dan tidak dapat dikembalikan.") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            histories.filter { selectedItems.contains(it.id) }.forEach {
                                viewModel.deleteHistory(it)
                            }
                            selectedItems = emptySet()
                            selectionMode = false
                            showDeleteDialog = false
                        }
                    }) {
                        Text("Hapus", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        if (showDeleteAllDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAllDialog = false },
                title = { Text("Hapus semua riwayat?") },
                text = { Text("Semua $historyCount riwayat makanan akan dihapus dan tidak dapat dikembalikan.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteAllHistories()
                        showDeleteAllDialog = false
                    }) {
                        Text("Hapus Semua", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAllDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

@Composable
private fun HistoryItemCard(
    history: ScanHistoryEntity,
    viewModel: HistoryViewModel,
    selectionMode: Boolean,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: ((ScanHistoryEntity, Bitmap?) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(history.id) {
        scope.launch {
            imageBitmap = viewModel.getHistoryImage(history)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (selectionMode) {
                    onSelectionChange(!isSelected)
                } else {
                    onClick?.invoke(history, imageBitmap)
                }
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.componentMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange
                )
                Spacer(modifier = Modifier.width(Spacing.componentSmall))
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(MaterialTheme.shapes.small)
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

            Spacer(modifier = Modifier.width(Spacing.componentMedium))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = history.mealName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${HistoryMapper.mealTypeOf(history).label} · ${formatTime(history.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = history.consumedEnergyKcal?.roundToInt()?.toString() ?: "-",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "kkal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!selectionMode) {
                TextButton(onClick = onDelete) {
                    Text(
                        text = "Hapus",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Belum ada riwayat",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.componentSmall))

            Text(
                text = "Setiap makanan yang Anda scan akan\ntersimpan otomatis di sini.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DayHeader(dayStart: Long, totalKcal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.componentMedium, bottom = Spacing.componentExtraTiny),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDayLabel(dayStart),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${NutritionFormatter.formatThousands(totalKcal)} kkal",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeeklyTrendCard(totals: List<DayTotal>, targetKcal: Double) {
    val average = DailyTotals.averageOfLoggedDays(totals)
    // Scale to the target as well, so a quiet week still shows how far below the target it is.
    val maxKcal = maxOf(targetKcal, totals.maxOfOrNull { it.energyKcal } ?: 0.0).coerceAtLeast(1.0)
    val todayStart = DailyTotals.startOfDay(System.currentTimeMillis())
    val chartHeight = 96.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.componentLarge)) {
            Text(
                text = "7 Hari Terakhir",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (average != null) {
                    "Rata-rata ${NutritionFormatter.formatThousands(average)} kkal/hari · " +
                        "target ${NutritionFormatter.formatThousands(targetKcal)} kkal"
                } else {
                    "Belum ada makanan tercatat minggu ini"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            Box(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                // Target line
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart)
                        .padding(bottom = chartHeight * (targetKcal / maxKcal).toFloat()),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall),
                    verticalAlignment = Alignment.Bottom
                ) {
                    totals.forEach { day ->
                        val fraction = (day.energyKcal / maxKcal).toFloat().coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(fraction.coerceAtLeast(0.02f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    when {
                                        day.entryCount == 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        day.energyKcal > targetKcal -> MaterialTheme.colorScheme.tertiary
                                        else -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall)
            ) {
                totals.forEach { day ->
                    val isToday = day.dayStartMillis == todayStart
                    Text(
                        text = if (isToday) "Ini" else SimpleDateFormat("EEE", Locale("id", "ID")).format(Date(day.dayStartMillis)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatDayLabel(dayStart: Long): String {
    val today = DailyTotals.startOfDay(System.currentTimeMillis())
    val yesterday = DailyTotals.startOfDay(today - 12 * 60 * 60 * 1000L)
    return when (dayStart) {
        today -> "Hari ini"
        yesterday -> "Kemarin"
        else -> SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID")).format(Date(dayStart))
    }
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH.mm", Locale("id", "ID")).format(Date(timestamp))