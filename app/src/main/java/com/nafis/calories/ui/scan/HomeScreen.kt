package com.nafis.calories.ui.scan

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.model.DailyTargetCalculator
import com.nafis.calories.data.model.DailyTargets
import com.nafis.calories.data.model.MealType
import com.nafis.calories.data.remote.api.RetrofitClient
import com.nafis.calories.data.remote.dto.DailyInsightResponseDto
import com.nafis.calories.data.repository.MealRecommendation
import com.nafis.calories.data.repository.MealRecommendationRepository
import com.nafis.calories.data.repository.MealRecommendations
import com.nafis.calories.data.repository.RemainingBudget
import com.nafis.calories.data.repository.PackagedProduct
import com.nafis.calories.data.repository.PackagedProductRepository
import com.nafis.calories.data.repository.UserProfileRepository
import com.nafis.calories.data.repository.WaterTrackerRepository
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.theme.CarbBgColor
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.ui.theme.WaterBgColor
import com.nafis.calories.ui.theme.WaterColor
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.utils.DailyTotals
import com.nafis.calories.utils.formatKcalInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onCameraClick: () -> Unit,
    onUploadClick: () -> Unit,
    onRealtimeScanClick: () -> Unit,
    historyViewModel: HistoryViewModel? = null,
    onHistoryItemClick: ((ScanHistoryEntity, Bitmap?) -> Unit)? = null,
    dailyTargets: DailyTargets = DailyTargetCalculator.default,
    onSetTargetClick: () -> Unit = {},
    scanViewModel: ScanViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    val histories by historyViewModel?.histories?.collectAsState(initial = emptyList())
        ?: remember { mutableStateOf(emptyList()) }

    // Today's total consumed calories and macros
    val todayStart = remember { DailyTotals.startOfDay(System.currentTimeMillis()) }
    val todayHistories = remember(histories) {
        histories.filter { it.timestamp >= todayStart }
    }

    val consumedKcal = remember(todayHistories) {
        todayHistories.sumOf { it.consumedEnergyKcal ?: 0.0 }.formatKcalInt()
    }
    val consumedCarb = remember(todayHistories) {
        todayHistories.sumOf { it.consumedCarbohydrateG ?: 0.0 }.formatKcalInt()
    }
    val consumedProt = remember(todayHistories) {
        todayHistories.sumOf { it.consumedProteinG ?: 0.0 }.formatKcalInt()
    }
    val consumedFat = remember(todayHistories) {
        todayHistories.sumOf { it.consumedFatG ?: 0.0 }.formatKcalInt()
    }

    val targetKcal = dailyTargets.energyKcal.formatKcalInt()
    val remainingKcal = (targetKcal - consumedKcal).coerceAtLeast(0)

    val waterRepo = remember { WaterTrackerRepository.getInstance(context) }
    val waterGlasses by waterRepo.waterGlasses.collectAsState()

    val userProfileRepo = remember { UserProfileRepository.getInstance(context) }
    val userProfile by userProfileRepo.profile.collectAsState()

    val waterTargetGlasses = dailyTargets.waterTargetGlasses

    // Only real backend content is shown: the card stays hidden until an insight arrives.
    var insightData by remember { mutableStateOf<DailyInsightResponseDto?>(null) }

    LaunchedEffect(consumedKcal, consumedProt, consumedCarb, consumedFat, waterGlasses, dailyTargets, userProfile) {
        val profile = userProfile ?: return@LaunchedEffect
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val response = RetrofitClient.api.getDailyInsights(
                    consumedKcal = consumedKcal.toDouble(),
                    targetKcal = targetKcal.toDouble(),
                    proteinG = consumedProt.toDouble(),
                    targetProteinG = dailyTargets.proteinG,
                    carbsG = consumedCarb.toDouble(),
                    targetCarbsG = dailyTargets.carbohydrateG,
                    fatG = consumedFat.toDouble(),
                    targetFatG = dailyTargets.fatG,
                    waterGlasses = waterGlasses,
                    targetGlasses = waterTargetGlasses,
                    mealCount = todayHistories.size,
                    goal = profile.goal.key
                )
                response.body()?.takeIf { response.isSuccessful }?.string()
                    ?.let { Gson().fromJson(it, DailyInsightResponseDto::class.java) }
            }.getOrNull()
        }
        // On failure keep the last insight that did load (still real data), if any.
        if (loaded != null && (!loaded.headline.isNullOrBlank() || !loaded.message.isNullOrBlank())) {
            insightData = loaded
        }
    }

    val formattedDate = remember {
        SimpleDateFormat("EEEE, d MMMM", Locale("id", "ID")).format(Date())
    }

    // Meal recommendations from GET /recommendations/meals, filtered by what is left today.
    var recommendationState by remember { mutableStateOf<RecommendationUiState>(RecommendationUiState.Loading) }
    var recommendationReloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(remainingKcal, consumedProt, consumedCarb, consumedFat, dailyTargets, userProfile, recommendationReloadKey) {
        val profile = userProfile ?: return@LaunchedEffect
        // Keep the current list visible while refreshing after a meal is logged.
        if (recommendationState !is RecommendationUiState.Loaded) {
            recommendationState = RecommendationUiState.Loading
        }
        val result = MealRecommendationRepository.fetch(
            remaining = RemainingBudget(
                energyKcal = remainingKcal.toDouble(),
                proteinG = dailyTargets.proteinG - consumedProt,
                carbohydrateG = dailyTargets.carbohydrateG - consumedCarb,
                fatG = dailyTargets.fatG - consumedFat
            ),
            mealType = MealType.fromHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)),
            goal = profile.goal,
            preferences = profile.dietaryPreferences,
            allergies = profile.allergies
        )
        recommendationState = result.fold(
            onSuccess = { RecommendationUiState.Loaded(it) },
            onFailure = { RecommendationUiState.Failed(it.message ?: "Rekomendasi menu belum bisa dimuat.") }
        )
    }

    val activeCategory = (recommendationState as? RecommendationUiState.Loaded)?.data?.category
        ?: insightData?.suggestedCategory?.takeIf { it.isNotBlank() }

    var showSearchSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        Spacer(modifier = Modifier.height(Spacing.screenTop))

        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Ringkasan Hari Ini",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(
                onClick = onSetTargetClick,
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profil & Target",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Radial Progress Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(124.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progressFraction = (consumedKcal.toFloat() / targetKcal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = trackColor,
                                radius = size.minDimension / 2 - 6.dp.toPx(),
                                style = Stroke(width = 11.dp.toPx())
                            )
                            drawArc(
                                color = Primary,
                                startAngle = -90f,
                                sweepAngle = 360f * progressFraction,
                                useCenter = false,
                                style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = remainingKcal.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "kkal tersisa",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Harian", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$targetKcal kkal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Terkonsumsi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$consumedKcal kkal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Primary)
                        }

                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hidrasi ($waterGlasses/$waterTargetGlasses)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(
                                modifier = Modifier.clickable { waterRepo.incrementWater(1) },
                                shape = RoundedCornerShape(8.dp),
                                color = WaterBgColor
                            ) {
                                Text(
                                    text = "+ 250ml",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = WaterColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroBarItem(
                        label = "Karbo",
                        consumedG = consumedCarb,
                        targetG = dailyTargets.carbohydrateG.formatKcalInt(),
                        barColor = CarbColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroBarItem(
                        label = "Protein",
                        consumedG = consumedProt,
                        targetG = dailyTargets.proteinG.formatKcalInt(),
                        barColor = ProteinColor,
                        modifier = Modifier.weight(1f)
                    )
                    MacroBarItem(
                        label = "Lemak",
                        consumedG = consumedFat,
                        targetG = dailyTargets.fatG.formatKcalInt(),
                        barColor = FatColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Coaching Insight Card (only when the backend sent one)
        insightData?.let { insight ->
            DailyInsightCard(insight = insight)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Quick Action Bar (Packaged Product Search & Quick Log)
        QuickActionBar(
            onSearchPackaged = { showSearchSheet = true },
            onQuickLog = {
                val lastItem = histories.firstOrNull()
                if (lastItem != null && historyViewModel != null) {
                    scope.launch {
                        val bmp = historyViewModel.getHistoryImage(lastItem)
                        onHistoryItemClick?.invoke(lastItem, bmp)
                    }
                } else {
                    showMessage("Belum ada menu sebelumnya untuk dicatat ulang.")
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Smart Meal Recommendations Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = activeCategory?.let { "Rekomendasi Menu ($it)" } ?: "Rekomendasi Menu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Sisa $remainingKcal kkal",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier.clickable {
                    showMessage("Rekomendasi disesuaikan dengan sisa kalori dan target harianmu")
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (val state = recommendationState) {
            RecommendationUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
            }

            is RecommendationUiState.Failed -> RecommendationMessage(
                text = state.message,
                actionLabel = "Coba lagi",
                onAction = { recommendationReloadKey++ }
            )

            is RecommendationUiState.Loaded -> if (state.data.items.isEmpty()) {
                RecommendationMessage(text = "Belum ada menu yang pas dengan sisa kalorimu hari ini.")
            } else {
                state.data.items.forEach { rec ->
                    MealRecommendationTile(
                        recommendation = rec,
                        onClick = {
                            if (scanViewModel == null) return@MealRecommendationTile
                            scanViewModel.logRecommendedMeal(rec) { success ->
                                showMessage(
                                    if (success) "${rec.title} ditambahkan ke catatan"
                                    else "Gagal mencatat ${rec.title}. Silakan coba lagi."
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Intake Log ("Catatan Asupan Hari Ini")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Catatan Asupan Hari Ini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Semua Riwayat",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier.clickable {
                    // Handled in MainActivity navigation
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (todayHistories.isEmpty()) {
                    Text(
                        text = "Belum ada makanan yang dicatat hari ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    todayHistories.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (onHistoryItemClick != null && historyViewModel != null) {
                                        scope.launch {
                                            val bmp = historyViewModel.getHistoryImage(item)
                                            onHistoryItemClick(item, bmp)
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.mealName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(item.timestamp))
                                val totalKcal = item.totalEnergyKcal
                                val consumedKcalVal = item.consumedEnergyKcal
                                val subtext = if (totalKcal != null && consumedKcalVal != null && (totalKcal - consumedKcalVal) > 50) {
                                    "$timeText • Porsi individu (dari total ${totalKcal.formatKcalInt()} kkal foto)"
                                } else {
                                    "$timeText • P ${(item.consumedProteinG ?: 0.0).formatKcalInt()}g • K ${(item.consumedCarbohydrateG ?: 0.0).formatKcalInt()}g • L ${(item.consumedFatG ?: 0.0).formatKcalInt()}g"
                                }
                                Text(
                                    text = subtext,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${item.consumedEnergyKcal.formatKcalInt()} kkal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (index < todayHistories.size - 1) {
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF1F5F9)))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(104.dp))
    }

    // Sits above the bottom navigation bar so it never overlaps the Scan FAB.
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 84.dp)
    )
    }

    if (showSearchSheet) {
        PackagedProductSearchSheet(
            onDismiss = { showSearchSheet = false },
            onLogProduct = { product, onDone ->
                if (scanViewModel == null) {
                    onDone(false)
                } else {
                    scanViewModel.logPackagedProduct(product) { success ->
                        onDone(success)
                        showMessage(
                            if (success) "${product.name} ditambahkan ke catatan"
                            else "Gagal mencatat ${product.name}. Silakan coba lagi."
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagedProductSearchSheet(
    onDismiss: () -> Unit,
    onLogProduct: (PackagedProduct, (Boolean) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<PackagedProduct>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val loggingIds = remember { mutableStateListOf<String>() }
    val loggedIds = remember { mutableStateListOf<String>() }

    fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.length < 2 || isLoading) return
        keyboard?.hide()
        isLoading = true
        errorMessage = null
        scope.launch {
            PackagedProductRepository.search(trimmed)
                .onSuccess { results = it }
                .onFailure {
                    results = null
                    errorMessage = it.message
                }
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Cari Produk Kemasan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ketik nama makanan/minuman kemasan atau nomor barcode untuk mengecek kandungan gizinya.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Contoh: susu UHT cokelat") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { runSearch() },
                    enabled = query.trim().length >= 2 && !isLoading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Cari", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val currentResults = results
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }

                errorMessage != null -> SheetInfoText(errorMessage!!)

                currentResults == null -> SheetInfoText(
                    "Hasil pencarian akan muncul di sini. Kamu bisa langsung mencatatnya ke riwayat hari ini."
                )

                currentResults.isEmpty() -> SheetInfoText(
                    "Produk tidak ditemukan. Coba kata kunci lain atau periksa kembali nomor barcode."
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentResults, key = { it.id }) { product ->
                        PackagedProductRow(
                            product = product,
                            isLogging = product.id in loggingIds,
                            isLogged = product.id in loggedIds,
                            onLog = {
                                loggingIds.add(product.id)
                                onLogProduct(product) { success ->
                                    loggingIds.remove(product.id)
                                    if (success) loggedIds.add(product.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetInfoText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    )
}

@Composable
private fun PackagedProductRow(
    product: PackagedProduct,
    isLogging: Boolean,
    isLogged: Boolean,
    onLog: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!product.brand.isNullOrBlank()) {
                    Text(
                        text = product.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${product.energyKcal.formatKcalInt()} kkal / ${product.servingLabel ?: "sajian"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary
                )
            }
            Button(
                onClick = onLog,
                enabled = !isLogging && !isLogged,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                when {
                    isLogging -> CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    isLogged -> Text("Tercatat", style = MaterialTheme.typography.labelMedium)
                    else -> Text("+ Catat", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DailyInsightCard(insight: DailyInsightResponseDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ProteinBgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Focus chip sits on its own line above the headline so neither gets truncated.
            if (!insight.priorityFocus.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Primary
                ) {
                    Text(
                        text = "Fokus: ${insight.priorityFocus}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Headline wraps to as many lines as needed: no ellipsis.
                Text(
                    text = insight.headline?.trim()?.takeIf { it.isNotEmpty() } ?: "Insight Harian",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            // Collapse blank lines from the backend so the card stays compact; omit when empty.
            insight.message?.trim()?.replace(Regex("\\s*\\n\\s*\\n+"), "\n")
                ?.takeIf { it.isNotEmpty() }
                ?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }

            if (!insight.hydrationNudge.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "💧 ${insight.hydrationNudge}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionBar(
    onSearchPackaged: () -> Unit,
    onQuickLog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onSearchPackaged() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = CarbBgColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = CarbColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cari Kemasan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Cek kalori produk",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onQuickLog() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = ProteinBgColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Catat Ulang",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Menu terakhir",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroBarItem(
    label: String,
    consumedG: Int,
    targetG: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (consumedG.toFloat() / targetG.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(barColor)
                    .padding(end = (1 - progress).dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "$consumedG / ${targetG}g",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp
        )
    }
}

private sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState
    data class Loaded(val data: MealRecommendations) : RecommendationUiState
    data class Failed(val message: String) : RecommendationUiState
}

@Composable
private fun RecommendationMessage(
    text: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.clickable(onClick = onAction)
                )
            }
        }
    }
}

@Composable
private fun MealRecommendationTile(
    recommendation: MealRecommendation,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clickable { onClick() },
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
                    text = recommendation.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        "${recommendation.energyKcal.formatKcalInt()} kkal",
                        "Protein ${recommendation.proteinG.formatKcalInt()}g",
                        recommendation.category
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                recommendation.reason?.let { reason ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = Primary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Tambah",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}