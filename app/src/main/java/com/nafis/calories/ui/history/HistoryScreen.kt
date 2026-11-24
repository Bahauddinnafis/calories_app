package com.nafis.calories.ui.history

import android.graphics.Bitmap
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.DangerColor
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.DailyTotals
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onHistoryItemClick: ((ScanHistoryEntity, Bitmap?) -> Unit)? = null,
    dailyTargetKcal: Double = DailyTargetCalculator.DEFAULT_ENERGY_KCAL
) {
    val histories by viewModel.histories.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val historiesByDay = remember(histories) {
        histories.groupBy { DailyTotals.startOfDay(it.timestamp) }
    }

    val weeklyTotals = remember(histories) {
        DailyTotals.lastDays(histories.map { it.timestamp to (it.consumedEnergyKcal ?: 0.0) })
    }

    val avgKcal = remember(weeklyTotals) {
        if (weeklyTotals.isNotEmpty()) {
            weeklyTotals.map { it.energyKcal }.average().roundToInt()
        } else {
            1720
        }
    }

    var pendingDeleteHistory by remember { mutableStateOf<ScanHistoryEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        // Screen Header
        Text(
            text = "Riwayat Asupan",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Consistency Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Konsistensi 7 Hari Terakhir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Rata-rata $avgKcal kkal",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dynamic 7-Day Bar Chart Grid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    val dayFormatter = SimpleDateFormat("EEE", Locale("id", "ID"))
                    val maxTarget = dailyTargetKcal.coerceAtLeast(1.0)

                    weeklyTotals.forEachIndexed { idx, dayTotal ->
                        val isToday = idx == weeklyTotals.size - 1
                        val dayLabel = if (isToday) "Hari Ini" else dayFormatter.format(Date(dayTotal.dayStartMillis))

                        val energyKcal = dayTotal.energyKcal
                        val fraction = (energyKcal / maxTarget).coerceIn(0.0, 1.25)
                        val barHeight = (70.dp * fraction.toFloat()).coerceAtLeast(8.dp)

                        val barColor = when {
                            energyKcal > maxTarget -> DangerColor
                            isToday -> CarbColor
                            else -> Primary
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(barColor)
                            )
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History Items List
        if (histories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada riwayat asupan tersimpan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                historiesByDay.forEach { (startOfDay, dayItems) ->
                    val dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID")).format(Date(startOfDay))

                    item {
                        Text(
                            text = dateFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(dayItems) { history ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val bmp = viewModel.getHistoryImage(history)
                                        onHistoryItemClick?.invoke(history, bmp)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = ProteinBgColor
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Restaurant,
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = history.mealName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(history.timestamp))
                                    Text(
                                        text = "$timeStr • K ${(history.consumedCarbohydrateG ?: 0.0).toInt()}g • P ${(history.consumedProteinG ?: 0.0).toInt()}g • L ${(history.consumedFatG ?: 0.0).toInt()}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "${(history.consumedEnergyKcal ?: 0.0).toInt()} kkal",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                IconButton(
                                    onClick = { pendingDeleteHistory = history },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = DangerColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Confirmation Dialog
        pendingDeleteHistory?.let { historyToDelete ->
            AlertDialog(
                onDismissRequest = { pendingDeleteHistory = null },
                title = { Text("Hapus Catatan") },
                text = { Text("Apakah Anda yakin ingin menghapus '${historyToDelete.mealName}' dari riwayat?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteHistory(historyToDelete)
                            pendingDeleteHistory = null
                        }
                    ) {
                        Text("Hapus", color = DangerColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteHistory = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}