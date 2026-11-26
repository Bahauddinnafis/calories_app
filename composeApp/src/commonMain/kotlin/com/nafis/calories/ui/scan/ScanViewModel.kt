package com.nafis.calories.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.model.MealType
import com.nafis.calories.domain.model.ScanCorrection
import com.nafis.calories.domain.model.ScanCorrectionRules
import com.nafis.calories.domain.model.ScanError
import com.nafis.calories.domain.model.ScanResult
import com.nafis.calories.domain.model.ScanStatus
import com.nafis.calories.domain.repository.FoodAnalysisRepository
import com.nafis.calories.domain.repository.ScanHistoryRepository
import com.nafis.calories.domain.usecase.GetScanDailyContextUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class ScanViewModel(
    private val foodAnalysis: FoodAnalysisRepository,
    private val history: ScanHistoryRepository,
    private val getDailyContext: GetScanDailyContextUseCase,
    private val clock: Clock
) : ViewModel() {

    private val log = Logger.withTag("ScanViewModel")

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Home)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var lastScanImage: ByteArray? = null
    private val pendingSaves = mutableMapOf<String, Job>()
    private var portionUpdateJob: Job? = null
    private var estimateJob: Job? = null

    fun openScanner() {
        _uiState.value = ScanUiState.Scanner
    }

    fun showHistoryDetail(entry: MealEntry, image: ByteArray?) {
        viewModelScope.launch {
            val scanResult = history.getScanResult(entry.id) ?: return@launch
            _uiState.value = ScanUiState.Success(scanResult, image, fromHistory = true)
        }
    }

    fun updateConsumedPortion(scanId: String, weightG: Double) {
        updateCurrentResult { it.copy(consumedWeightG = weightG) }
        portionUpdateJob?.cancel()
        portionUpdateJob = viewModelScope.launch {
            delay(PORTION_SAVE_DEBOUNCE_MS)
            pendingSaves[scanId]?.join()
            if (!history.updateConsumedWeight(scanId, weightG)) log.w { "No history row to update portion for scan $scanId" }
        }
    }

    fun startCorrection() {
        val state = _uiState.value
        val (result, image) = when (state) {
            is ScanUiState.Success -> state.scanResult to state.capturedImage
            is ScanUiState.Warning -> state.scanResult to state.capturedImage
            is ScanUiState.NutritionUnavailable -> state.scanResult to state.capturedImage
            else -> return
        }
        _uiState.value = ScanUiState.Editing(original = result, capturedImage = image, returnTo = state)
    }

    fun cancelCorrection() {
        val state = _uiState.value as? ScanUiState.Editing ?: return
        if (!state.isSubmitting) _uiState.value = state.returnTo
    }

    fun submitCorrection(correction: ScanCorrection) {
        val editing = _uiState.value as? ScanUiState.Editing ?: return
        if (editing.isSubmitting) return

        ScanCorrectionRules.validate(correction)?.let { message ->
            _uiState.value = editing.copy(errorMessage = message)
            return
        }

        estimateJob?.cancel()
        val submitting = editing.copy(isSubmitting = true, errorMessage = null)
        _uiState.value = submitting
        // The user may leave while waiting; never pull them back into this flow.
        fun stillWaiting() = _uiState.value == submitting

        viewModelScope.launch {
            val original = editing.original
            val recalculated = foodAnalysis.recalculate(original.scanId, correction).getOrElse { error ->
                if (stillWaiting()) _uiState.value = editing.copy(
                    isSubmitting = false,
                    errorMessage = error.message ?: "Perhitungan ulang belum berhasil. Silakan coba lagi."
                )
                return@launch
            }.copy(
                consumedWeightG = null,
                mealType = original.mealType,
                timestamp = original.timestamp
            ).let { it.copy(consumedWeightG = it.defaultConsumedWeightG) }

            if (recalculated.status == ScanStatus.NUTRITION_UNAVAILABLE) {
                if (stillWaiting()) _uiState.value = editing.copy(
                    isSubmitting = false,
                    errorMessage = "Data gizi untuk bahan tersebut belum ditemukan. " +
                        "Coba nama yang lebih umum, misalnya \"nasi putih\" atau \"telur ayam goreng\"."
                )
                return@launch
            }

            pendingSaves[original.scanId]?.join()
            history.saveCorrection(recalculated, editing.capturedImage).onFailure {
                log.e(it) { "Failed to save corrected scan ${original.scanId}" }
            }

            if (!stillWaiting()) return@launch
            val fromHistory = (editing.returnTo as? ScanUiState.Success)?.fromHistory == true
            _uiState.value = if (recalculated.status == ScanStatus.PARTIAL) {
                ScanUiState.Warning(recalculated, editing.capturedImage, buildPartialWarning(recalculated))
            } else {
                ScanUiState.Success(recalculated, editing.capturedImage, fromHistory = fromHistory)
            }
        }
    }

    fun previewCorrection(correction: ScanCorrection) {
        val editing = _uiState.value as? ScanUiState.Editing ?: return
        if (editing.isSubmitting) return
        estimateJob?.cancel()
        if (ScanCorrectionRules.validate(correction) != null) {
            _uiState.value = editing.copy(estimateKcal = null, isEstimating = false, estimateUnavailable = false)
            return
        }
        _uiState.value = editing.copy(isEstimating = true, estimateUnavailable = false)
        estimateJob = viewModelScope.launch {
            delay(ESTIMATE_DEBOUNCE_MS)
            val result = foodAnalysis.estimate(correction)
            val current = _uiState.value as? ScanUiState.Editing ?: return@launch
            if (current.isSubmitting || current.original.scanId != editing.original.scanId) return@launch
            _uiState.value = current.copy(
                estimateKcal = result.getOrNull()?.totalNutrition?.energyKcal,
                isEstimating = false,
                estimateUnavailable = result.isFailure
            )
        }
    }

    fun scanFood(jpegImage: ByteArray) {
        lastScanImage = jpegImage
        _uiState.value = ScanUiState.Loading
        viewModelScope.launch { performScan(jpegImage) }
    }

    fun retryScan() {
        lastScanImage?.let(::scanFood)
    }

    fun resetState() {
        lastScanImage = null
        _uiState.value = ScanUiState.Home
    }

    private suspend fun performScan(image: ByteArray) {
        val scanResult = foodAnalysis.scan(image, getDailyContext()).getOrElse { error ->
            log.w(error) { "Scan failed" }
            _uiState.value = ScanUiState.Error(
                message = error.message ?: "Analisis belum berhasil. Periksa koneksi internet Anda, lalu coba lagi.",
                canRetry = true
            )
            return
        }.let { it.copy(consumedWeightG = it.defaultConsumedWeightG, mealType = MealType.fromHour(currentHour())) }

        if (scanResult.status == ScanStatus.NUTRITION_UNAVAILABLE) {
            _uiState.value = ScanUiState.NutritionUnavailable(
                scanResult = scanResult,
                capturedImage = image,
                message = describeErrors(scanResult.errors),
                canRetry = scanResult.isRetryable
            )
            return
        }

        pendingSaves[scanResult.scanId] = viewModelScope.launch {
            history.saveScan(scanResult, image).onFailure { log.e(it) { "Failed to save history" } }
            pendingSaves.remove(scanResult.scanId)
        }

        _uiState.value = if (scanResult.status == ScanStatus.PARTIAL) {
            ScanUiState.Warning(scanResult, image, buildPartialWarning(scanResult))
        } else {
            ScanUiState.Success(scanResult, image)
        }
    }

    private fun updateCurrentResult(transform: (ScanResult) -> ScanResult) {
        _uiState.value = when (val state = _uiState.value) {
            is ScanUiState.Success -> state.copy(scanResult = transform(state.scanResult))
            is ScanUiState.Warning -> state.copy(scanResult = transform(state.scanResult))
            else -> state
        }
    }

    private fun buildPartialWarning(scanResult: ScanResult): String {
        val missingIngredients = scanResult.ingredients.count { !it.hasNutrition }
        return if (missingIngredients > 0) {
            "Data gizi untuk $missingIngredients dari ${scanResult.ingredients.size} bahan belum tersedia, " +
                "jadi total kalori di bawah mungkin lebih rendah dari sebenarnya."
        } else {
            "Sebagian data gizi belum tersedia, jadi angka di bawah mungkin belum lengkap."
        }
    }

    private fun describeErrors(errors: List<ScanError>): String = when (errors.firstOrNull()?.code) {
        "provider_timeout" -> "Layanan sedang lambat merespons. Silakan coba lagi."
        "provider_unavailable" -> "Layanan sedang tidak tersedia. Coba lagi dalam beberapa saat."
        "invalid_provider_response" -> "Perhitungan kalori belum berhasil kali ini. Silakan coba lagi."
        "estimation_failed" -> "Makanannya sudah dikenali, tetapi kalorinya belum bisa diperkirakan. " +
            "Coba koreksi nama makanan atau bahannya dengan nama yang lebih umum."
        else -> "Makanannya sudah dikenali, tetapi data gizinya belum tersedia."
    }

    private fun currentHour(): Int = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

    private companion object {
        const val PORTION_SAVE_DEBOUNCE_MS = 400L
        const val ESTIMATE_DEBOUNCE_MS = 500L
    }
}

sealed interface ScanUiState {
    data object Home : ScanUiState
    data object Scanner : ScanUiState
    data object Loading : ScanUiState

    data class Success(
        val scanResult: ScanResult,
        val capturedImage: ByteArray?,
        val fromHistory: Boolean = false
    ) : ScanUiState

    data class Warning(
        val scanResult: ScanResult,
        val capturedImage: ByteArray?,
        val warning: String
    ) : ScanUiState

    data class NutritionUnavailable(
        val scanResult: ScanResult,
        val capturedImage: ByteArray?,
        val message: String,
        val canRetry: Boolean
    ) : ScanUiState

    data class Error(
        val message: String,
        val canRetry: Boolean = false
    ) : ScanUiState

    data class Editing(
        val original: ScanResult,
        val capturedImage: ByteArray?,
        val returnTo: ScanUiState,
        val isSubmitting: Boolean = false,
        val errorMessage: String? = null,
        val estimateKcal: Double? = null,
        val isEstimating: Boolean = false,
        val estimateUnavailable: Boolean = false
    ) : ScanUiState
}