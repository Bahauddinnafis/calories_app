package com.nafis.calories.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafis.calories.domain.model.MealEntry
import com.nafis.calories.domain.repository.ScanHistoryRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val repository: ScanHistoryRepository) : ViewModel() {

    val entries: Flow<List<MealEntry>> = repository.observeEntries()
    val entryCount: Flow<Int> = repository.observeEntryCount()

    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Idle)
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    suspend fun loadImage(entry: MealEntry): ByteArray? = repository.loadImage(entry)

    fun delete(entry: MealEntry) = runDelete("Gagal menghapus.") {
        repository.delete(entry.id)
    }

    fun deleteAll() = runDelete("Gagal menghapus riwayat.") {
        repository.deleteAll()
    }

    fun clearDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    private fun runDelete(errorMessage: String, delete: suspend () -> Result<*>) {
        viewModelScope.launch {
            _deleteState.value = DeleteState.Deleting
            _deleteState.value = if (delete().isSuccess) DeleteState.Success else DeleteState.Error(errorMessage)
            delay(STATUS_DISPLAY_MS)
            _deleteState.value = DeleteState.Idle
        }
    }

    private companion object {
        const val STATUS_DISPLAY_MS = 2_000L
    }
}

sealed interface DeleteState {
    data object Idle : DeleteState
    data object Deleting : DeleteState
    data object Success : DeleteState
    data class Error(val message: String) : DeleteState
}