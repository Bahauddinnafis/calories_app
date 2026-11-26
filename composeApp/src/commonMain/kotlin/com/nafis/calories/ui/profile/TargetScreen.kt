package com.nafis.calories.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafis.calories.domain.model.WeightGoal
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.theme.CarbBgColor
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.FatBgColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.NutritionFormatter
import kotlin.math.roundToInt

@Composable
fun TargetScreen(
    viewModel: ProfileViewModel,
    onEditProfiling: () -> Unit
) {
    val scrollState = rememberScrollState()
    val savedProfile by viewModel.profile.collectAsStateWithLifecycle()
    val dailyTargets by viewModel.dailyTargets.collectAsStateWithLifecycle()

    val profile = savedProfile
    val goalText = when (profile?.goal) {
        WeightGoal.LOSE -> "Menurunkan Berat Badan (Defisit Kalori)"
        WeightGoal.MAINTAIN -> "Menjaga Berat Badan (Maintenance)"
        WeightGoal.GAIN -> "Menaikkan Berat Badan & Otot (Surplus)"
        null -> "Profil belum dilengkapi"
    }
    fun kg(value: Double?): String = value?.let { "${formatKg(it)} kg" } ?: "–"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Target Kalori & Makro",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                modifier = Modifier.clickable { onEditProfiling() },
                shape = RoundedCornerShape(10.dp),
                color = ProteinBgColor,
                border = BorderStroke(1.dp, Primary)
            ) {
                Text(
                    text = "Ubah Target",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Anggaran Kalori Harian",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = dailyTargets.energyKcal.roundToInt().toString(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF5EEAD4)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kkal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFCBD5E1),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TDEE ${dailyTargets.tdeeKcal.toInt()} kkal • Fokus: $goalText",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Distribusi Makronutrisi Harian",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = CarbBgColor
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Karbohidrat",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = CarbColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dailyTargets.carbohydrateG.roundToInt()}g",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dailyTargets.carbohydratePct}% total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = ProteinBgColor
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Protein",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ProteinColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dailyTargets.proteinG.roundToInt()}g",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dailyTargets.proteinPct}% total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = FatBgColor
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Lemak",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = FatColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${dailyTargets.fatG.roundToInt()}g",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${dailyTargets.fatPct}% total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Berat Saat Ini",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = kg(profile?.weightKg),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Target Berat Badan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = kg(profile?.targetWeightKg),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Laju Perubahan Mingguan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = profile?.weeklyPaceKg?.let { "${formatKg(it)} kg / minggu" } ?: "–",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        SecondaryButton(
            text = "Hitung Ulang Target & Profiling",
            onClick = onEditProfiling,
            icon = Icons.Outlined.Edit
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}

internal fun formatKg(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else NutritionFormatter.formatDecimal(rounded)
}