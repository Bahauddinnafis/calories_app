package com.nafis.calories.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.model.AddOnSelection
import com.nafis.calories.data.model.IngredientCorrection
import com.nafis.calories.data.model.ScanCorrection
import com.nafis.calories.data.model.ScanCorrectionRules
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.data.model.SuggestedAddOn
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.Spacing
import kotlin.math.roundToInt

private const val WEIGHT_STEP_G = 20.0
private const val MIN_WEIGHT_G = 10.0

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CorrectionScreen(
    original: ScanResult,
    isSubmitting: Boolean,
    errorMessage: String?,
    estimateKcal: Double?,
    isEstimating: Boolean,
    estimateUnavailable: Boolean,
    onCorrectionChanged: (ScanCorrection) -> Unit,
    onSubmit: (ScanCorrection) -> Unit,
    onCancel: () -> Unit
) {
    val scrollState = rememberScrollState()

    var mealName by remember(original.scanId) { mutableStateOf(original.mealName) }

    // Starts from the detected ingredients, scaled to the portion eaten. Empty when none were detected.
    val ingredientList = remember(original.scanId) {
        mutableStateListOf<IngredientCorrection>().apply {
            addAll(ScanCorrectionRules.prefill(original).ingredients)
        }
    }

    // Energy density (kcal/g) of each detected ingredient, used only to label rows.
    val kcalPerGramByName = remember(original.scanId) {
        original.ingredients.mapNotNull { ing ->
            val kcal = ing.nutrition?.energyKcal ?: return@mapNotNull null
            if (ing.estimatedWeightG <= 0.0) return@mapNotNull null
            ing.foodName.trim().lowercase() to kcal / ing.estimatedWeightG
        }.toMap()
    }

    val addOnOptions = original.suggestedAddOns
    val selectedAddOnIds = remember(original.scanId) { mutableStateListOf<String>() }

    val correction = ScanCorrection(
        mealName = mealName,
        ingredients = ingredientList.toList(),
        addOns = addOnOptions
            .filter { it.addOnId in selectedAddOnIds }
            .map { AddOnSelection(addOnId = it.addOnId, weightG = it.defaultWeightG) }
    )

    // Ask the backend for the new total whenever the correction changes (debounced in the ViewModel).
    LaunchedEffect(correction) { onCorrectionChanged(correction) }

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

        // Top Header Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Batal",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Text(
                    text = "Koreksi Porsi & Bahan",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(38.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sesuaikan berat bahan atau tambahkan komponen tersembunyi, lalu kalorinya dihitung ulang.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it },
            label = { Text("Nama Makanan Utama") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Ingredient Steppers Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (ingredientList.isEmpty()) {
                    Text(
                        text = "Belum ada bahan yang terdeteksi. Tambahkan bahan beserta beratnya di bawah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                ingredientList.forEachIndexed { index, item ->
                    IngredientStepperRow(
                        item = item,
                        kcalPerGram = kcalPerGramByName[item.foodName.trim().lowercase()],
                        onWeightChange = { ingredientList[index] = item.copy(weightG = it) },
                        onRemove = { ingredientList.removeAt(index) }
                    )
                    if (index < ingredientList.size - 1) {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))
                Spacer(modifier = Modifier.height(8.dp))

                AddIngredientRow(
                    enabled = ingredientList.size < ScanCorrectionRules.MAX_INGREDIENTS,
                    onAdd = { name, weight -> ingredientList.add(IngredientCorrection(name, weight)) }
                )
            }
        }

        // Hidden calorie add-ons come from the backend for this dish; hidden when it suggests none.
        if (addOnOptions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Tambahan Kalori Tersembunyi",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                addOnOptions.forEach { option ->
                    AddOnChip(
                        option = option,
                        isSelected = option.addOnId in selectedAddOnIds,
                        onToggle = {
                            if (option.addOnId in selectedAddOnIds) selectedAddOnIds.remove(option.addOnId)
                            else selectedAddOnIds.add(option.addOnId)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Backend estimate for the edited correction (requested by the ViewModel)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimasi Setelah Koreksi",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    when {
                        isEstimating -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                        estimateKcal != null -> Text(
                            text = "${estimateKcal.roundToInt()} kkal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                        else -> Text(
                            text = "–",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (estimateUnavailable && !isEstimating) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Estimasi belum tersedia. Total akhir tetap dihitung saat koreksi diterapkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF1F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE11D48))
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE11D48),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = if (isSubmitting) "Menghitung ulang…" else "Hitung Ulang & Terapkan",
            onClick = { onSubmit(correction) },
            enabled = !isSubmitting
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun IngredientStepperRow(
    item: IngredientCorrection,
    kcalPerGram: Double?,
    onWeightChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.foodName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (kcalPerGram != null) {
                Text(
                    text = "≈ ${(kcalPerGram * item.weightG).roundToInt()} kkal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFF4F6F8), RoundedCornerShape(10.dp))
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StepperButton(
                icon = Icons.Default.Remove,
                description = "Kurangi berat",
                onClick = { onWeightChange((item.weightG - WEIGHT_STEP_G).coerceAtLeast(MIN_WEIGHT_G)) }
            )
            Text(
                text = "${item.weightG.roundToInt()}g",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.Center
            )
            StepperButton(
                icon = Icons.Default.Add,
                description = "Tambah berat",
                onClick = {
                    onWeightChange((item.weightG + WEIGHT_STEP_G).coerceAtMost(ScanCorrectionRules.MAX_WEIGHT_G))
                }
            )
        }

        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Hapus ${item.foodName}",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(28.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = description, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddIngredientRow(
    enabled: Boolean,
    onAdd: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    val weight = weightText.replace(',', '.').toDoubleOrNull()
    val canAdd = enabled && ScanCorrectionRules.isValidName(name) && ScanCorrectionRules.isValidWeight(weight)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Nama bahan") },
            singleLine = true,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
        )
        OutlinedTextField(
            value = weightText,
            onValueChange = { input -> weightText = input.filter { it.isDigit() || it == '.' || it == ',' } },
            placeholder = { Text("Gram") },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(88.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
        )
        TextButton(
            onClick = {
                if (canAdd && weight != null) {
                    onAdd(name.trim(), weight)
                    name = ""
                    weightText = ""
                }
            },
            enabled = canAdd
        ) {
            Text("Tambah", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddOnChip(
    option: SuggestedAddOn,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val label = option.energyKcal?.let { "${option.name} (+${it.roundToInt()} kkal)" } ?: option.name
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) ProteinBgColor else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Primary else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.clickable(onClick = onToggle)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}