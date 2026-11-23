package com.nafis.calories.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nafis.calories.data.model.ActivityLevel
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.Sex
import com.nafis.calories.data.model.UserProfile
import com.nafis.calories.data.model.WeightGoal
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.NutritionFormatter

/** Collects the data needed to personalise the daily calorie target. Stored on-device only. */
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onSaved: () -> Unit
) {
    val saved by viewModel.profile.collectAsState()

    var sex by rememberSaveable { mutableStateOf(saved?.sex) }
    var ageText by rememberSaveable { mutableStateOf(saved?.ageYears?.toString().orEmpty()) }
    var heightText by rememberSaveable { mutableStateOf(saved?.heightCm?.let(::formatNumber).orEmpty()) }
    var weightText by rememberSaveable { mutableStateOf(saved?.weightKg?.let(::formatNumber).orEmpty()) }
    var activity by rememberSaveable { mutableStateOf(saved?.activityLevel) }
    var goal by rememberSaveable { mutableStateOf(saved?.goal) }

    val age = ageText.toIntOrNull()
    val height = heightText.replace(',', '.').toDoubleOrNull()
    val weight = weightText.replace(',', '.').toDoubleOrNull()

    val ageError = ageText.isNotEmpty() && (age == null || age !in UserProfile.AGE_RANGE)
    val heightError = heightText.isNotEmpty() && (height == null || height !in UserProfile.HEIGHT_RANGE_CM)
    val weightError = weightText.isNotEmpty() && (weight == null || weight !in UserProfile.WEIGHT_RANGE_KG)

    val draft = if (
        sex != null && activity != null && goal != null &&
        age != null && height != null && weight != null &&
        !ageError && !heightError && !weightError
    ) {
        UserProfile(sex!!, age, height, weight, activity!!, goal!!)
    } else {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
        Text(
            text = "Dipakai untuk menghitung target kalori harian Anda. Data hanya tersimpan di perangkat ini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionTitle("Jenis kelamin")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
            Sex.entries.forEach { option ->
                ChoiceCard(
                    title = option.label,
                    selected = sex == option,
                    onClick = { sex = option },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SectionTitle("Data tubuh")
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
            NumberField(
                value = ageText,
                onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                label = "Usia",
                suffix = "thn",
                isError = ageError,
                decimal = false,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = heightText,
                onValueChange = { heightText = sanitizeDecimal(it) },
                label = "Tinggi",
                suffix = "cm",
                isError = heightError,
                modifier = Modifier.weight(1f)
            )
            NumberField(
                value = weightText,
                onValueChange = { weightText = sanitizeDecimal(it) },
                label = "Berat",
                suffix = "kg",
                isError = weightError,
                modifier = Modifier.weight(1f)
            )
        }
        val bodyError = when {
            ageError -> "Usia harus antara ${UserProfile.AGE_RANGE.first}–${UserProfile.AGE_RANGE.last} tahun."
            heightError -> "Tinggi harus antara 100–250 cm."
            weightError -> "Berat harus antara 30–300 kg."
            else -> null
        }
        if (bodyError != null) {
            Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
            Text(
                text = bodyError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        SectionTitle("Seberapa aktif Anda?")
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
            ActivityLevel.entries.forEach { option ->
                ChoiceCard(
                    title = option.label,
                    description = option.description,
                    selected = activity == option,
                    onClick = { activity = option },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionTitle("Tujuan Anda")
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.componentSmall)) {
            WeightGoal.entries.forEach { option ->
                ChoiceCard(
                    title = option.label,
                    selected = goal == option,
                    onClick = { goal = option },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.sectionLarge))

        if (draft != null) {
            val targets = DailyTargetCalculator.calculate(draft)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.componentLarge)) {
                    Text(
                        text = "Target harian Anda",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${NutritionFormatter.formatThousands(targets.energyKcal)} kkal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Protein ${targets.proteinG.toInt()} g · Karbohidrat ${targets.carbohydrateG.toInt()} g · " +
                            "Lemak ${targets.fatG.toInt()} g",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.componentExtraTiny))
                    Text(
                        text = "Perkiraan umum untuk orang dewasa sehat. Untuk kondisi khusus (hamil, menyusui, " +
                            "atau penyakit tertentu), konsultasikan dengan tenaga kesehatan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.componentLarge))
        } else {
            Text(
                text = "Lengkapi semua data di atas untuk melihat target harian Anda.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.componentMedium))
        }

        PrimaryButton(
            text = if (saved == null) "Simpan Profil" else "Simpan Perubahan",
            onClick = {
                draft?.let {
                    viewModel.saveProfile(it)
                    onSaved()
                }
            },
            enabled = draft != null && draft != saved
        )

        Spacer(modifier = Modifier.height(Spacing.screenBottom))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(Spacing.sectionLarge))
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(Spacing.componentSmall))
}

@Composable
private fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.componentSmall, vertical = Spacing.componentSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.padding(start = Spacing.componentSmall)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
    decimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text(suffix) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier
    )
}

private fun sanitizeDecimal(input: String): String {
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
    }.take(6)
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')