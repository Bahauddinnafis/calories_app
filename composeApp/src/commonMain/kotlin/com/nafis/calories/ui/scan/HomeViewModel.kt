package com.nafis.calories.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafis.calories.domain.model.DailyInsight
import com.nafis.calories.domain.model.DailyInsightQuery
import com.nafis.calories.domain.model.DailyTargetCalculator
import com.nafis.calories.domain.model.DailyTargets
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealRecommendation
import com.nafis.calories.domain.model.MealRecommendations
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.PackagedProduct
import com.nafis.calories.domain.model.RemainingBudget
import com.nafis.calories.domain.model.UserProfile
import com.nafis.calories.domain.model.formatKcalInt
import com.nafis.calories.domain.repository.InsightRepository
import com.nafis.calories.domain.repository.MealRecommendationRepository
import com.nafis.calories.domain.repository.PackagedProductRepository
import com.nafis.calories.domain.repository.ScanHistoryRepository
import com.nafis.calories.domain.repository.UserProfileRepository
import com.nafis.calories.domain.repository.WaterRepository
import com.nafis.calories.domain.usecase.LogMealUseCase
import com.nafis.calories.utils.DailyTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class DailySummary(
    val entries: List<MealEntry> = emptyList(),
    val energyKcal: Int = 0,
    val proteinG: Int = 0,
    val carbohydrateG: Int = 0,
    val fatG: Int = 0
)

sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState
    data class Loaded(val data: MealRecommendations) : RecommendationUiState
    data class Failed(val message: String) : RecommendationUiState
}

data class HomeUiState(
    val allEntries: List<MealEntry> = emptyList(),
    val today: DailySummary = DailySummary(),
    val targets: DailyTargets = DailyTargetCalculator.default,
    val profile: UserProfile? = null,
    val waterGlasses: Int = 0,
    val insight: DailyInsight? = null,
    val recommendations: RecommendationUiState = RecommendationUiState.Loading
) {
    val targetKcal: Int get() = targets.energyKcal.formatKcalInt()
    val remainingKcal: Int get() = (targetKcal - today.energyKcal).coerceAtLeast(0)
    val activeCategory: String?
        get() = (recommendations as? RecommendationUiState.Loaded)?.data?.category
            ?: insight?.suggestedCategory?.takeIf { it.isNotBlank() }
}

private data class CoachingInput(
    val today: DailySummary,
    val targets: DailyTargets,
    val profile: UserProfile,
    val waterGlasses: Int
)

class HomeViewModel(
    private val history: ScanHistoryRepository,
    profileRepository: UserProfileRepository,
    private val waterRepository: WaterRepository,
    private val insightRepository: InsightRepository,
    private val recommendationRepository: MealRecommendationRepository,
    private val productRepository: PackagedProductRepository,
    private val logMeal: LogMealUseCase,
    private val clock: Clock
) : ViewModel() {

    private val insight = MutableStateFlow<DailyInsight?>(null)
    private val recommendations = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Loading)
    private val recommendationReload = MutableStateFlow(0)

    private val entries = history.observeEntries()
    private val today = entries.map(::summarizeToday).distinctUntilChanged()

    val uiState: StateFlow<HomeUiState> = combine(
        combine(entries, today) { all, summary -> all to summary },
        combine(profileRepository.profile, profileRepository.dailyTargets) { profile, targets -> profile to targets },
        waterRepository.waterGlasses,
        insight,
        recommendations
    ) { (all, summary), (profile, targets), water, insight, recommendations ->
        HomeUiState(all, summary, targets, profile, water, insight, recommendations)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        val coachingInput = combine(
            today,
            profileRepository.dailyTargets,
            profileRepository.profile.filterNotNull(),
            waterRepository.waterGlasses
        ) { summary, targets, profile, water -> CoachingInput(summary, targets, profile, water) }
            .distinctUntilChanged()

        viewModelScope.launch {
            coachingInput.collectLatest { input ->
                insightRepository.fetchDailyInsight(input.toInsightQuery()).onSuccess { insight.value = it }
            }
        }

        val recommendationInput = coachingInput.map { it.copy(waterGlasses = 0) }.distinctUntilChanged()
        viewModelScope.launch {
            combine(recommendationInput, recommendationReload) { input, _ -> input }.collectLatest { input ->
                if (recommendations.value !is RecommendationUiState.Loaded) {
                    recommendations.value = RecommendationUiState.Loading
                }
                recommendations.value = fetchRecommendations(input)
            }
        }
    }

    fun reloadRecommendations() {
        recommendationReload.value++
    }

    fun addWaterGlass() {
        waterRepository.addGlasses(1)
    }

    fun refreshWater() {
        waterRepository.refresh()
    }

    suspend fun loadImage(entry: MealEntry): ByteArray? = history.loadImage(entry)

    suspend fun searchProducts(query: String): Result<List<PackagedProduct>> = productRepository.search(query)

    fun logRecommendation(meal: MealRecommendation, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(logMeal.logRecommendation(meal).isSuccess) }
    }

    fun logProduct(product: PackagedProduct, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(logMeal.logProduct(product).isSuccess) }
    }

    private suspend fun fetchRecommendations(input: CoachingInput): RecommendationUiState {
        val (summary, targets, profile) = input
        return recommendationRepository.fetch(
            remaining = RemainingBudget(
                energyKcal = (targets.energyKcal.formatKcalInt() - summary.energyKcal).coerceAtLeast(0).toDouble(),
                proteinG = targets.proteinG - summary.proteinG,
                carbohydrateG = targets.carbohydrateG - summary.carbohydrateG,
                fatG = targets.fatG - summary.fatG
            ),
            mealType = MealType.fromHour(clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour),
            goal = profile.goal,
            preferences = profile.dietaryPreferences,
            allergies = profile.allergies
        ).fold(
            onSuccess = { RecommendationUiState.Loaded(it) },
            onFailure = { RecommendationUiState.Failed(it.message ?: "Rekomendasi menu belum bisa dimuat.") }
        )
    }

    private fun summarizeToday(all: List<MealEntry>): DailySummary {
        val start = DailyTotals.startOfDay(clock.now().toEpochMilliseconds())
        val todays = all.filter { it.timestamp >= start }
        return DailySummary(
            entries = todays,
            energyKcal = todays.sumOf { it.energyKcal ?: 0.0 }.formatKcalInt(),
            proteinG = todays.sumOf { it.proteinG ?: 0.0 }.formatKcalInt(),
            carbohydrateG = todays.sumOf { it.carbohydrateG ?: 0.0 }.formatKcalInt(),
            fatG = todays.sumOf { it.fatG ?: 0.0 }.formatKcalInt()
        )
    }

    private fun CoachingInput.toInsightQuery() = DailyInsightQuery(
        consumedKcal = today.energyKcal.toDouble(),
        targetKcal = targets.energyKcal.formatKcalInt().toDouble(),
        proteinG = today.proteinG.toDouble(),
        targetProteinG = targets.proteinG,
        carbohydrateG = today.carbohydrateG.toDouble(),
        targetCarbohydrateG = targets.carbohydrateG,
        fatG = today.fatG.toDouble(),
        targetFatG = targets.fatG,
        waterGlasses = waterGlasses,
        targetGlasses = targets.waterTargetGlasses,
        mealCount = today.entries.size,
        goal = profile.goal
    )
}