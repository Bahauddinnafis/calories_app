package com.nafis.calories.ui.scan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nafis.calories.domain.model.DailyInsight
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealRecommendation
import com.nafis.calories.domain.model.PackagedProduct
import com.nafis.calories.domain.model.formatKcalInt
import com.nafis.calories.ui.theme.CarbBgColor
import com.nafis.calories.ui.theme.CarbColor
import com.nafis.calories.ui.theme.FatColor
import com.nafis.calories.ui.theme.Primary
import com.nafis.calories.ui.theme.ProteinBgColor
import com.nafis.calories.ui.theme.ProteinColor
import com.nafis.calories.ui.theme.Spacing
import com.nafis.calories.ui.theme.WaterBgColor
import com.nafis.calories.ui.theme.WaterColor
import com.nafis.calories.utils.DateFormat
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onEntryClick: (MealEntry, ByteArray?) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    fun openEntry(entry: MealEntry) {
        scope.launch { onEntryClick(entry, viewModel.loadImage(entry)) }
    }

    val today = state.today
    val targets = state.targets
    val targetKcal = state.targetKcal
    val remainingKcal = state.remainingKcal
    val activeCategory = state.activeCategory
    val formattedDate = remember { DateFormat.weekdayDayMonth(DateFormat.nowMillis()) }

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
                    onClick = onOpenProfile,
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
                            val progressFraction = (today.energyKcal.toFloat() / targetKcal.coerceAtLeast(1)).coerceIn(0f, 1f)
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
                                Text("${today.energyKcal} kkal", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Primary)
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Hidrasi (${state.waterGlasses}/${targets.waterTargetGlasses})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    modifier = Modifier.clickable { viewModel.addWaterGlass() },
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
                            consumedG = today.carbohydrateG,
                            targetG = targets.carbohydrateG.formatKcalInt(),
                            barColor = CarbColor,
                            modifier = Modifier.weight(1f)
                        )
                        MacroBarItem(
                            label = "Protein",
                            consumedG = today.proteinG,
                            targetG = targets.proteinG.formatKcalInt(),
                            barColor = ProteinColor,
                            modifier = Modifier.weight(1f)
                        )
                        MacroBarItem(
                            label = "Lemak",
                            consumedG = today.fatG,
                            targetG = targets.fatG.formatKcalInt(),
                            barColor = FatColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            state.insight?.let { insight ->
                DailyInsightCard(insight = insight)
                Spacer(modifier = Modifier.height(12.dp))
            }

            QuickActionBar(
                onSearchPackaged = { showSearchSheet = true },
                onQuickLog = {
                    val lastItem = state.allEntries.firstOrNull()
                    if (lastItem != null) {
                        openEntry(lastItem)
                    } else {
                        showMessage("Belum ada menu sebelumnya untuk dicatat ulang.")
                    }
                }
            )

            Spacer(modifier = Modifier.height(20.dp))

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

            when (val recommendations = state.recommendations) {
                RecommendationUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Primary)
                }

                is RecommendationUiState.Failed -> RecommendationMessage(
                    text = recommendations.message,
                    actionLabel = "Coba lagi",
                    onAction = viewModel::reloadRecommendations
                )

                is RecommendationUiState.Loaded -> if (recommendations.data.items.isEmpty()) {
                    RecommendationMessage(text = "Belum ada menu yang pas dengan sisa kalorimu hari ini.")
                } else {
                    recommendations.data.items.forEach { rec ->
                        MealRecommendationTile(
                            recommendation = rec,
                            onClick = {
                                viewModel.logRecommendation(rec) { success ->
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
                    modifier = Modifier.clickable(onClick = onOpenHistory)
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
                    if (today.entries.isEmpty()) {
                        Text(
                            text = "Belum ada makanan yang dicatat hari ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        today.entries.forEachIndexed { index, item ->
                            IntakeRow(entry = item, onClick = { openEntry(item) })

                            if (index < today.entries.size - 1) {
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
            onSearch = viewModel::searchProducts,
            onLogProduct = { product, onDone ->
                viewModel.logProduct(product) { success ->
                    onDone(success)
                    showMessage(
                        if (success) "${product.name} ditambahkan ke catatan"
                        else "Gagal mencatat ${product.name}. Silakan coba lagi."
                    )
                }
            }
        )
    }
}

@Composable
private fun IntakeRow(entry: MealEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.mealName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val totalKcal = entry.totalEnergyKcal
            val eatenKcal = entry.energyKcal
            val time = DateFormat.time(entry.timestamp)
            Text(
                text = if (totalKcal != null && eatenKcal != null && totalKcal - eatenKcal > 50) {
                    "$time • Porsi individu (dari total ${totalKcal.formatKcalInt()} kkal foto)"
                } else {
                    "$time • P ${entry.proteinG.formatKcalInt()}g • " +
                        "K ${entry.carbohydrateG.formatKcalInt()}g • L ${entry.fatG.formatKcalInt()}g"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${entry.energyKcal.formatKcalInt()} kkal",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PackagedProductSearchSheet(
    onDismiss: () -> Unit,
    onSearch: suspend (String) -> Result<List<PackagedProduct>>,
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
            onSearch(trimmed)
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
            val currentError = errorMessage
            when {
                isLoading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }

                currentError != null -> SheetInfoText(currentError)

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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
private fun DailyInsightCard(insight: DailyInsight) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ProteinBgColor),
        border = BorderStroke(1.dp, Primary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                Text(
                    text = insight.headline?.trim()?.takeIf { it.isNotEmpty() } ?: "Insight Harian",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
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
private fun QuickActionBar(
    onSearchPackaged: () -> Unit,
    onQuickLog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionTile(
            title = "Cari Kemasan",
            subtitle = "Cek kalori produk",
            icon = Icons.Default.Search,
            iconTint = CarbColor,
            iconBackground = CarbBgColor,
            onClick = onSearchPackaged,
            modifier = Modifier.weight(1f)
        )
        QuickActionTile(
            title = "Catat Ulang",
            subtitle = "Menu terakhir",
            icon = Icons.Default.FlashOn,
            iconTint = Primary,
            iconBackground = ProteinBgColor,
            onClick = onQuickLog,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
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
                color = iconBackground
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

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
            .clickable(onClick = onClick),
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