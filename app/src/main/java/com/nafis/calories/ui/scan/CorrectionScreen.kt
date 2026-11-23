package com.nafis.calories.ui.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.model.IngredientCorrection
import com.nafis.calories.data.model.ScanCorrection
import com.nafis.calories.data.model.ScanCorrectionRules
import com.nafis.calories.data.model.ScanResult
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.components.SecondaryButton
import com.nafis.calories.ui.theme.Spacing
import kotlin.math.roundToInt

private class IngredientRow(val key: Int, name: String, weightText: String) {
    var name by mutableStateOf(name)
    var weightText by mutableStateOf(weightText)
    val weightG: Double? get() = weightText.replace(',', '.').toDoubleOrNull()
}

@Composable
fun CorrectionScreen(
    original: ScanResult,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSubmit: (ScanCorrection) -> Unit,
    onCancel: () -> Unit
) {
    val initial = remember(original.scanId) { ScanCorrectionRules.prefill(original) }
    var mealName by remember(original.scanId) { mutableStateOf(initial.mealName) }
    var nextKey by remember(original.scanId) { mutableStateOf(initial.ingredients.size) }
    val rows = remember(original.scanId) {
        mutableStateListOf<IngredientRow>().apply {
            initial.ingredients.forEachIndexed { index, item ->
                add(IngredientRow(index, item.foodName, formatWeight(item.weightG)))
            }
            // Nothing detected (e.g. nutrition unavailable with no ingredients): start with one empty row.
            if (isEmpty()) add(IngredientRow(-1, "", ""))
        }
    }
    var showFieldErrors by remember(original.scanId) { mutableStateOf(false) }

    val totalWeight = rows.sumOf { it.weightG ?: 0.0 }
    val correction = ScanCorrection(
        mealName = mealName,
        ingredients = rows.map { IngredientCorrection(it.name, it.weightG ?: 0.0) }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel, enabled = !isSubmitting) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                text = "Koreksi Makanan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
        Text(
            text = "Ubah nama atau berat jika hasilnya kurang tepat. Isi berat yang Anda makan, " +
                "dan gunakan nama umum dalam bahasa Indonesia, misalnya \"nasi putih\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Spacing.sectionMedium))

        OutlinedTextField(
            value = mealName,
            onValueChange = { mealName = it.take(ScanCorrectionRules.MAX_NAME_LENGTH) },
            label = { Text("Nama makanan") },
            singleLine = true,
            enabled = !isSubmitting,
            isError = showFieldErrors && !ScanCorrectionRules.isValidName(mealName),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Spacing.sectionMedium))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Bahan",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total ${formatWeight(totalWeight)} g",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(Spacing.componentSmall))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
            rows.forEach { row ->
                // Keyed so deleting a row doesn't shift text-field state onto its neighbour.
                key(row.key) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = row.name,
                        onValueChange = { row.name = it.take(ScanCorrectionRules.MAX_NAME_LENGTH) },
                        placeholder = { Text("Nama bahan") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        isError = showFieldErrors && !ScanCorrectionRules.isValidName(row.name),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(Spacing.componentSmall))
                    OutlinedTextField(
                        value = row.weightText,
                        onValueChange = { row.weightText = sanitizeWeight(it) },
                        placeholder = { Text("0") },
                        suffix = { Text("g") },
                        singleLine = true,
                        enabled = !isSubmitting,
                        isError = showFieldErrors && !ScanCorrectionRules.isValidWeight(row.weightG),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.width(96.dp)
                    )
                    IconButton(
                        onClick = { rows.remove(row) },
                        enabled = !isSubmitting && rows.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Hapus ${row.name.ifBlank { "bahan" }}"
                        )
                    }
                }
                }
            }
        }

        TextButton(
            onClick = {
                rows.add(IngredientRow(nextKey, "", ""))
                nextKey++
            },
            enabled = !isSubmitting && rows.size < ScanCorrectionRules.MAX_INGREDIENTS
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(Spacing.componentExtraTiny))
            Text("Tambah Bahan")
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(Spacing.componentSmall))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                )
            ) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Spacing.componentMedium)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sectionMedium))

        Text(
            text = "Hasil koreksi akan menggantikan hasil sebelumnya di riwayat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Spacing.componentMedium))

        if (isSubmitting) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.buttonHeightLarge),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(Spacing.componentSmall))
                Text("Menghitung ulang…", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            PrimaryButton(
                text = "Hitung Ulang",
                icon = Icons.Filled.Calculate,
                onClick = {
                    showFieldErrors = true
                    onSubmit(correction)
                }
            )
            Spacer(modifier = Modifier.height(Spacing.componentSmall))
            SecondaryButton(text = "Batal", onClick = onCancel)
        }

        Spacer(modifier = Modifier.height(Spacing.screenBottom))
    }
}

private fun sanitizeWeight(input: String): String {
    var separatorSeen = false
    return input.filter { char ->
        when {
            char.isDigit() -> true
            (char == ',' || char == '.') && !separatorSeen -> {
                separatorSeen = true
                true
            }
            else -> false
        }
    }.take(7)
}

private fun formatWeight(value: Double): String =
    if (kotlin.math.abs(value - value.roundToInt()) < 0.05) {
        value.roundToInt().toString()
    } else {
        String.format(java.util.Locale("id", "ID"), "%.1f", value)
    }