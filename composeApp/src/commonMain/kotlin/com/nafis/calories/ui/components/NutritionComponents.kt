package com.nafis.calories.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nafis.calories.domain.model.DailyTargetCalculator
import com.nafis.calories.domain.model.Nutrition
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.NutrientLevel
import com.nafis.calories.utils.NutritionFormatter
import kotlin.math.roundToInt

@Composable
fun CalorieDisplay(
    calories: Double?,
    modifier: Modifier = Modifier,
    dailyEnergyKcal: Double = DailyTargetCalculator.DEFAULT_ENERGY_KCAL
) {
    val calVal = calories ?: 0.0
    val dailyPercent = ((calVal / dailyEnergyKcal.coerceAtLeast(1.0)) * 100).roundToInt().coerceAtLeast(0)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = NutritionFormatter.formatEnergy(calories),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(Spacing.componentExtraTiny))
            Text(
                text = "kkal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = Spacing.componentSmall)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Total kalori",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (calVal > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "±$dailyPercent% kebutuhan harian",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MacroNutrientsRow(
    protein: Double?,
    carbs: Double?,
    fat: Double?,
    modifier: Modifier = Modifier
) {
    val pVal = protein ?: 0.0
    val cVal = carbs ?: 0.0
    val fVal = fat ?: 0.0
    val totalGrams = (pVal + cVal + fVal).coerceAtLeast(1.0)

    val pRatio = (pVal / totalGrams).toFloat()
    val cRatio = (cVal / totalGrams).toFloat()
    val fRatio = (fVal / totalGrams).toFloat()

    val pPercent = (pRatio * 100).roundToInt()
    val cPercent = (cRatio * 100).roundToInt()
    val fPercent = (fRatio * 100).roundToInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.componentMedium)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                if (pRatio > 0) {
                    Box(modifier = Modifier.weight(pRatio).height(10.dp).background(ProteinColor))
                }
                if (cRatio > 0) {
                    Box(modifier = Modifier.weight(cRatio).height(10.dp).background(CarbColor))
                }
                if (fRatio > 0) {
                    Box(modifier = Modifier.weight(fRatio).height(10.dp).background(FatColor))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.componentMedium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroItem(label = "Protein", value = protein, percent = pPercent, unit = "g", color = ProteinColor)
                MacroItem(label = "Karbohidrat", value = carbs, percent = cPercent, unit = "g", color = CarbColor)
                MacroItem(label = "Lemak", value = fat, percent = fPercent, unit = "g", color = FatColor)
            }
        }
    }
}

@Composable
private fun MacroItem(
    label: String,
    value: Double?,
    percent: Int,
    unit: String,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(Spacing.componentExtraTiny))
            Text(
                text = "$label ($percent%)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = NutritionFormatter.formatMacro(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KeyNutrientsSection(
    nutrition: Nutrition,
    modifier: Modifier = Modifier
) {
    val sugarG = nutrition.sugarG
    val sodiumMg = nutrition.sodiumMg
    val saturatedFatG = nutrition.saturatedFatG
    val cholesterolMg = nutrition.cholesterolMg

    if (sugarG == null && sodiumMg == null && saturatedFatG == null && cholesterolMg == null) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.componentMedium)) {
            Text(
                text = "Perlu Diperhatikan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Dibandingkan dengan batas konsumsi harian",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.componentSmall))

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
                if (sugarG != null) {
                    LaypersonNutrientItem(
                        title = "Gula",
                        valueText = NutritionFormatter.formatSugarSdmEquivalent(sugarG),
                        level = NutritionFormatter.getSugarLevel(sugarG)
                    )
                }
                if (sodiumMg != null) {
                    LaypersonNutrientItem(
                        title = "Garam (natrium)",
                        valueText = NutritionFormatter.formatSaltEquivalent(sodiumMg),
                        level = NutritionFormatter.getSodiumLevel(sodiumMg)
                    )
                }
                if (saturatedFatG != null) {
                    LaypersonNutrientItem(
                        title = "Lemak jenuh",
                        valueText = "${NutritionFormatter.formatMacro(saturatedFatG)} g",
                        level = NutritionFormatter.getSaturatedFatLevel(saturatedFatG)
                    )
                }
                if (cholesterolMg != null) {
                    LaypersonNutrientItem(
                        title = "Kolesterol",
                        valueText = "${NutritionFormatter.formatMineral(cholesterolMg)} mg",
                        level = NutritionFormatter.getCholesterolLevel(cholesterolMg)
                    )
                }
            }
        }
    }
}

@Composable
private fun LaypersonNutrientItem(
    title: String,
    valueText: String,
    level: NutrientLevel?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            if (level != null) {
                Text(
                    text = level.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (level != null) {
            Spacer(modifier = Modifier.width(8.dp))
            NutrientLevelBadge(level = level)
        }
    }
}

@Composable
private fun NutrientLevelBadge(level: NutrientLevel) {
    val (bgColor, textColor) = when (level) {
        NutrientLevel.LOW -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        NutrientLevel.MODERATE -> Pair(Color(0xFFFFF8E1), Color(0xFFF57F17))
        NutrientLevel.HIGH -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
    }

    Surface(shape = CircleShape, color = bgColor) {
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MicroNutrientCard(
    label: String,
    value: Double?,
    unit: String,
    dailyTarget: Double,
    modifier: Modifier = Modifier
) {
    val valDouble = value ?: 0.0
    val progress = (valDouble / dailyTarget).toFloat().coerceIn(0f, 1f)
    val dailyPercent = ((valDouble / dailyTarget) * 100).roundToInt()

    val formattedValue = when {
        label == "Serat" -> NutritionFormatter.formatFiber(value)
        unit == "mg" -> NutritionFormatter.formatMineral(value)
        else -> NutritionFormatter.formatMacro(value)
    }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(Spacing.componentMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$dailyPercent% harian",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}