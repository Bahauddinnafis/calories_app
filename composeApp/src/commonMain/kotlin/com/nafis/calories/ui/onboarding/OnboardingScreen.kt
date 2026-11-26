package com.nafis.calories.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nafis.calories.domain.model.ActivityLevel
import com.nafis.calories.domain.model.DailyTargetCalculator
import com.nafis.calories.domain.model.DailyTargets
import com.nafis.calories.domain.model.Sex
import com.nafis.calories.domain.model.UserProfile
import com.nafis.calories.domain.model.WeightGoal
import com.nafis.calories.ui.components.PrimaryButton
import com.nafis.calories.ui.theme.CarbBgColor
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.DangerBgColor
import com.nafis.calories.ui.theme.DangerColor
import com.nafis.calories.ui.theme.FatBgColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.ui.theme.Spacing
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialProfile: UserProfile? = null,
    onComplete: (UserProfile) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var userName by remember { mutableStateOf(initialProfile?.userName ?: "") }
    var goal by remember { mutableStateOf(initialProfile?.goal ?: WeightGoal.LOSE) }
    var targetWeightKg by remember { mutableStateOf(initialProfile?.targetWeightKg) }
    var weeklyPaceKg by remember { mutableDoubleStateOf(initialProfile?.weeklyPaceKg ?: 0.50) }

    var sex by remember { mutableStateOf(initialProfile?.sex ?: Sex.MALE) }
    var ageYears by remember { mutableStateOf(initialProfile?.ageYears) }
    var heightCm by remember { mutableStateOf(initialProfile?.heightCm) }
    var weightKg by remember { mutableStateOf(initialProfile?.weightKg) }

    var activityLevel by remember { mutableStateOf(initialProfile?.activityLevel ?: ActivityLevel.LIGHT) }

    var selectedDietary by remember {
        mutableStateOf(
            initialProfile?.dietaryPreferences ?: emptySet()
        )
    }
    var selectedAllergies by remember {
        mutableStateOf(
            initialProfile?.allergies ?: emptySet()
        )
    }

    var breakfastTime by remember { mutableStateOf(initialProfile?.breakfastTime ?: "07:30") }
    var lunchTime by remember { mutableStateOf(initialProfile?.lunchTime ?: "12:30") }
    var dinnerTime by remember { mutableStateOf(initialProfile?.dinnerTime ?: "19:00") }

    var hydrationGoalEnabled by remember { mutableStateOf(initialProfile?.hydrationGoalEnabled ?: true) }
    var calorieWarningEnabled by remember { mutableStateOf(initialProfile?.calorieWarningEnabled ?: true) }

    val currentDraftProfile = remember(
        userName, sex, ageYears, heightCm, weightKg, activityLevel, goal,
        targetWeightKg, weeklyPaceKg, selectedDietary, selectedAllergies,
        breakfastTime, lunchTime, dinnerTime, hydrationGoalEnabled, calorieWarningEnabled
    ) {
        val age = ageYears
        val height = heightCm
        val weight = weightKg
        if (!isValidName(userName) || !isValidAge(age) || !isValidHeight(height) || !isValidWeight(weight)) {
            return@remember null
        }
        UserProfile(
            userName = userName.trim(),
            sex = sex,
            ageYears = age!!,
            heightCm = height!!,
            weightKg = weight!!,
            activityLevel = activityLevel,
            goal = goal,
            targetWeightKg = if (goal == WeightGoal.MAINTAIN) weight else targetWeightKg ?: weight,
            weeklyPaceKg = weeklyPaceKg,
            dietaryPreferences = selectedDietary,
            allergies = selectedAllergies,
            breakfastTime = breakfastTime,
            lunchTime = lunchTime,
            dinnerTime = dinnerTime,
            hydrationGoalEnabled = hydrationGoalEnabled,
            calorieWarningEnabled = calorieWarningEnabled,
            isOnboardingCompleted = true
        )
    }

    val dailyTargets = remember(currentDraftProfile) {
        currentDraftProfile?.let { DailyTargetCalculator.calculate(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenHorizontal)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(Spacing.screenTop))

            if (step in 1..5) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        IconButton(
                            onClick = { step-- },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(38.dp))
                    }

                    Text(
                        text = "Langkah $step dari 5",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(38.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { step / 5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = Primary,
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingStepTransition"
            ) { targetStep ->
                when (targetStep) {
                    1 -> Step1Goal(
                        selectedGoal = goal,
                        targetWeight = targetWeightKg,
                        weeklyPace = weeklyPaceKg,
                        onGoalSelected = { goal = it },
                        onTargetWeightChanged = { targetWeightKg = it },
                        onWeeklyPaceChanged = { weeklyPaceKg = it },
                        onNext = { step = 2 }
                    )
                    2 -> Step2Biometrics(
                        userName = userName,
                        sex = sex,
                        age = ageYears,
                        height = heightCm,
                        weight = weightKg,
                        onUserNameChanged = { userName = it },
                        onSexChanged = { sex = it },
                        onAgeChanged = { ageYears = it },
                        onHeightChanged = { heightCm = it },
                        onWeightChanged = { weightKg = it },
                        onNext = { step = 3 }
                    )
                    3 -> Step3Activity(
                        selectedActivity = activityLevel,
                        onActivitySelected = { activityLevel = it },
                        onNext = { step = 4 }
                    )
                    4 -> Step4Dietary(
                        selectedDietary = selectedDietary,
                        selectedAllergies = selectedAllergies,
                        onDietaryToggled = { pref ->
                            selectedDietary = if (selectedDietary.contains(pref)) {
                                selectedDietary - pref
                            } else {
                                selectedDietary + pref
                            }
                        },
                        onAllergyToggled = { allergy ->
                            selectedAllergies = if (selectedAllergies.contains(allergy)) {
                                selectedAllergies - allergy
                            } else {
                                selectedAllergies + allergy
                            }
                        },
                        onNext = { step = 5 }
                    )
                    5 -> Step5Schedule(
                        breakfastTime = breakfastTime,
                        lunchTime = lunchTime,
                        dinnerTime = dinnerTime,
                        hydrationGoalEnabled = hydrationGoalEnabled,
                        calorieWarningEnabled = calorieWarningEnabled,
                        onBreakfastTimeChanged = { breakfastTime = it },
                        onLunchTimeChanged = { lunchTime = it },
                        onDinnerTimeChanged = { dinnerTime = it },
                        onHydrationToggled = { hydrationGoalEnabled = it },
                        onCalorieWarningToggled = { calorieWarningEnabled = it },
                        onNext = { step = 6 }
                    )
                    6 -> {
                        val draft = currentDraftProfile
                        val targets = dailyTargets
                        if (draft != null && targets != null) {
                            Step6Summary(
                                profile = draft,
                                targets = targets,
                                onStartApp = { onComplete(draft) }
                            )
                        } else {
                            LaunchedEffect(Unit) { step = 2 }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Goal(
    selectedGoal: WeightGoal,
    targetWeight: Double?,
    weeklyPace: Double,
    onGoalSelected: (WeightGoal) -> Unit,
    onTargetWeightChanged: (Double?) -> Unit,
    onWeeklyPaceChanged: (Double) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Apa fokus utama kesehatan Anda?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Kami menyesuaikan kebutuhan kalori dan distribusi makronutrisi harian berdasarkan target Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        ChoiceTile(
            title = "Menurunkan Berat Badan",
            desc = "Defisit kalori terkontrol untuk mengurangi lemak tubuh",
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            isSelected = selectedGoal == WeightGoal.LOSE,
            onClick = { onGoalSelected(WeightGoal.LOSE) }
        )
        ChoiceTile(
            title = "Menjaga Berat Badan",
            desc = "Menyeimbangkan asupan energi dan metabolisme harian",
            icon = Icons.AutoMirrored.Filled.TrendingFlat,
            isSelected = selectedGoal == WeightGoal.MAINTAIN,
            onClick = { onGoalSelected(WeightGoal.MAINTAIN) }
        )
        ChoiceTile(
            title = "Menaikkan Berat Badan",
            desc = "Surplus kalori terukur dengan rasio protein optimal",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            isSelected = selectedGoal == WeightGoal.GAIN,
            onClick = { onGoalSelected(WeightGoal.GAIN) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var targetText by remember { mutableStateOf(targetWeight?.let(::formatNumber).orEmpty()) }
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { input ->
                            targetText = input.filter { it.isDigit() || it == '.' || it == ',' }
                            onTargetWeightChanged(parseNumber(targetText))
                        },
                        placeholder = { Text("Contoh: 65") },
                        isError = targetText.isNotEmpty() && !isValidWeight(targetWeight),
                        enabled = selectedGoal != WeightGoal.MAINTAIN,
                        label = { Text("Target Berat (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary
                        )
                    )

                    var expandedPace by remember { mutableStateOf(false) }
                    val paceOptions = listOf(0.25, 0.50, 0.75)

                    ExposedDropdownMenuBox(
                        expanded = expandedPace,
                        onExpandedChange = { expandedPace = !expandedPace },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = "$weeklyPace kg / minggu",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Laju Mingguan", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            maxLines = 1,
                            shape = RoundedCornerShape(14.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPace) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expandedPace,
                            onDismissRequest = { expandedPace = false }
                        ) {
                            paceOptions.forEach { pace ->
                                DropdownMenuItem(
                                    text = { Text("$pace kg / minggu", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        onWeeklyPaceChanged(pace)
                                        expandedPace = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(
            text = "Lanjutkan",
            onClick = onNext,
            enabled = selectedGoal == WeightGoal.MAINTAIN || isValidWeight(targetWeight)
        )
    }
}

@Composable
private fun Step2Biometrics(
    userName: String,
    sex: Sex,
    age: Int?,
    height: Double?,
    weight: Double?,
    onUserNameChanged: (String) -> Unit,
    onSexChanged: (Sex) -> Unit,
    onAgeChanged: (Int?) -> Unit,
    onHeightChanged: (Double?) -> Unit,
    onWeightChanged: (Double?) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Data biometrik dasar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Digunakan untuk menghitung Basal Metabolic Rate (BMR) menggunakan formula klinis Mifflin-St Jeor.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = userName,
                    onValueChange = onUserNameChanged,
                    label = { Text("Nama Panggilan", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )

                Column {
                    Text(
                        text = "Jenis Kelamin Biologis",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sex == Sex.MALE) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { onSexChanged(Sex.MALE) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Laki-laki",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sex == Sex.MALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (sex == Sex.FEMALE) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { onSexChanged(Sex.FEMALE) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Perempuan",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (sex == Sex.FEMALE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                var ageText by remember { mutableStateOf(age?.toString().orEmpty()) }
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { input ->
                        ageText = input.filter { it.isDigit() }.take(3)
                        onAgeChanged(ageText.toIntOrNull())
                    },
                    isError = ageText.isNotEmpty() && !isValidAge(age),
                    supportingText = if (ageText.isNotEmpty() && !isValidAge(age)) {
                        { Text("Usia ${UserProfile.AGE_RANGE.first}–${UserProfile.AGE_RANGE.last} tahun") }
                    } else null,
                    label = { Text("Usia (Tahun)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var heightText by remember { mutableStateOf(height?.let(::formatNumber).orEmpty()) }
                    OutlinedTextField(
                        value = heightText,
                        onValueChange = { input ->
                            heightText = input.filter { it.isDigit() || it == '.' || it == ',' }
                            onHeightChanged(parseNumber(heightText))
                        },
                        isError = heightText.isNotEmpty() && !isValidHeight(height),
                        label = { Text("Tinggi Badan (cm)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                    )

                    var weightText by remember { mutableStateOf(weight?.let(::formatNumber).orEmpty()) }
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { input ->
                            weightText = input.filter { it.isDigit() || it == '.' || it == ',' }
                            onWeightChanged(parseNumber(weightText))
                        },
                        isError = weightText.isNotEmpty() && !isValidWeight(weight),
                        label = { Text("Berat Saat Ini (kg)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        maxLines = 1,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(
            text = "Lanjutkan",
            onClick = onNext,
            enabled = isValidName(userName) && isValidAge(age) && isValidHeight(height) && isValidWeight(weight)
        )
    }
}

@Composable
private fun Step3Activity(
    selectedActivity: ActivityLevel,
    onActivitySelected: (ActivityLevel) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Tingkat aktivitas harian",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Menentukan koefisien pengali Total Daily Energy Expenditure (TDEE) Anda.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        ChoiceTile(
            title = ActivityLevel.SEDENTARY.label,
            desc = ActivityLevel.SEDENTARY.description,
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            isSelected = selectedActivity == ActivityLevel.SEDENTARY,
            onClick = { onActivitySelected(ActivityLevel.SEDENTARY) }
        )
        ChoiceTile(
            title = ActivityLevel.LIGHT.label,
            desc = ActivityLevel.LIGHT.description,
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            isSelected = selectedActivity == ActivityLevel.LIGHT,
            onClick = { onActivitySelected(ActivityLevel.LIGHT) }
        )
        ChoiceTile(
            title = ActivityLevel.MODERATE.label,
            desc = ActivityLevel.MODERATE.description,
            icon = Icons.Default.FitnessCenter,
            isSelected = selectedActivity == ActivityLevel.MODERATE,
            onClick = { onActivitySelected(ActivityLevel.MODERATE) }
        )
        ChoiceTile(
            title = ActivityLevel.HIGH.label,
            desc = ActivityLevel.HIGH.description,
            icon = Icons.Default.FlashOn,
            isSelected = selectedActivity == ActivityLevel.HIGH,
            onClick = { onActivitySelected(ActivityLevel.HIGH) }
        )

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(text = "Lanjutkan", onClick = onNext)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step4Dietary(
    selectedDietary: Set<String>,
    selectedAllergies: Set<String>,
    onDietaryToggled: (String) -> Unit,
    onAllergyToggled: (String) -> Unit,
    onNext: () -> Unit
) {
    val dietaryOptions = listOf(
        "Masakan Nusantara", "Tinggi Protein", "Halal", "Rendah Karbo", "Vegetarian"
    )
    val allergyOptions = listOf(
        "Makanan Laut (Seafood)", "Kacang-kacangan", "Produk Susu / Laktosa", "Telur"
    )

    Column {
        Text(
            text = "Preferensi & batasan pangan",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Digunakan mesin rekomendasi untuk menyaring menu lokal yang relevan dan aman dikonsumsi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column {
                    Text(
                        text = "Preferensi Pola Makan",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dietaryOptions.forEach { pref ->
                            val selected = selectedDietary.contains(pref)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) ProteinBgColor else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (selected) Primary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.clickable { onDietaryToggled(pref) }
                            ) {
                                Text(
                                    text = pref,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Alergi & Bahan yang Dihindari",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allergyOptions.forEach { allergy ->
                            val selected = selectedAllergies.contains(allergy)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) DangerBgColor else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (selected) DangerColor else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier.clickable { onAllergyToggled(allergy) }
                            ) {
                                Text(
                                    text = allergy,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) DangerColor else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(text = "Lanjutkan", onClick = onNext)
    }
}

@Composable
private fun Step5Schedule(
    breakfastTime: String,
    lunchTime: String,
    dinnerTime: String,
    hydrationGoalEnabled: Boolean,
    calorieWarningEnabled: Boolean,
    onBreakfastTimeChanged: (String) -> Unit,
    onLunchTimeChanged: (String) -> Unit,
    onDinnerTimeChanged: (String) -> Unit,
    onHydrationToggled: (Boolean) -> Unit,
    onCalorieWarningToggled: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "Jadwal makan & pengingat",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Atur parameter harian agar aplikasi memicu pengingat pemindaian tepat sebelum waktu makan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Jadwal Notifikasi Pemindaian",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sarapan",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = breakfastTime,
                            onValueChange = onBreakfastTimeChanged,
                            singleLine = true,
                            maxLines = 1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Makan Siang",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = lunchTime,
                            onValueChange = onLunchTimeChanged,
                            singleLine = true,
                            maxLines = 1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Makan Malam",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = dinnerTime,
                            onValueChange = onDinnerTimeChanged,
                            singleLine = true,
                            maxLines = 1,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Primary)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Target Hidrasi Harian",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pelacakan 2.000 ml (8 gelas) per hari",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = hydrationGoalEnabled,
                        onCheckedChange = onHydrationToggled,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
                    )
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Peringatan Batas Kalori",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Notifikasi jika asupan melampaui target",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = calorieWarningEnabled,
                        onCheckedChange = onCalorieWarningToggled,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(text = "Buat Rencana Nutrisi", onClick = onNext)
    }
}

@Composable
private fun Step6Summary(
    profile: UserProfile,
    targets: DailyTargets,
    onStartApp: () -> Unit
) {
    Column {
        Text(
            text = "ANALISIS PROFIL SELESAI",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Rencana Nutrisi Personal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Estimasi pencapaian target dalam 24 minggu",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

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
                        text = targets.energyKcal.toInt().toString(),
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
                    text = "TDEE ${targets.tdeeKcal.toInt()} kkal • Defisit ${targets.deficitOrSurplusKcal.toInt().let { if (it < 0) -it else it }} kkal/hari",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Distribusi Makronutrisi Harian",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = CarbBgColor
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Karbohidrat",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CarbColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${targets.carbohydrateG.toInt()}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "45% total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = ProteinBgColor
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Protein",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProteinColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${targets.proteinG.toInt()}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "30% total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = FatBgColor
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Lemak",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = FatColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${targets.fatG.toInt()}g",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "25% total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Nama Pengguna", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.userName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Jadwal Pengingat", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${profile.breakfastTime}, ${profile.lunchTime}, ${profile.dinnerTime}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Hidrasi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("2.000 ml (8 Gelas)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Filter Rekomendasi", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(profile.dietaryPreferences.take(2).joinToString(" • "), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(text = "Mulai Gunakan Aplikasi", onClick = onStartApp)
    }
}

@Composable
private fun ChoiceTile(
    title: String,
    desc: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) ProteinBgColor else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) Primary else Color.Transparent
        ),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(13.dp),
                color = if (isSelected) Primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Primary else Color.Transparent)
                    .border(2.dp, if (isSelected) Primary else MaterialTheme.colorScheme.outline, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

private fun isValidName(name: String): Boolean = name.trim().length in 1..50

private fun isValidAge(age: Int?): Boolean = age != null && age in UserProfile.AGE_RANGE

private fun isValidHeight(heightCm: Double?): Boolean = heightCm != null && heightCm in UserProfile.HEIGHT_RANGE_CM

private fun isValidWeight(weightKg: Double?): Boolean = weightKg != null && weightKg in UserProfile.WEIGHT_RANGE_KG

private fun parseNumber(text: String): Double? = text.replace(',', '.').toDoubleOrNull()

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')