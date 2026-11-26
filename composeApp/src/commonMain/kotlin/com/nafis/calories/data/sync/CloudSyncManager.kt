package com.nafis.calories.data.sync

import co.touchlab.kermit.Logger
import com.nafis.calories.core.config.platformName
import com.nafis.calories.core.storage.KeyValueStoreFactory
import com.nafis.calories.data.local.dao.ScanHistoryDao
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.dto.AuthResponseDto
import com.nafis.calories.data.remote.dto.BasicNutritionDto
import com.nafis.calories.data.remote.dto.MealLogRequestDto
import com.nafis.calories.data.remote.dto.MealLogResponseDto
import com.nafis.calories.data.remote.dto.ProfileResponseDto
import com.nafis.calories.data.remote.dto.WaterLogRequestDto
import com.nafis.calories.data.remote.dto.WaterTodayDto
import com.nafis.calories.data.remote.session.SessionStore
import com.nafis.calories.data.repository.UserProfileRepositoryImpl
import com.nafis.calories.data.repository.WaterRepositoryImpl
import com.nafis.calories.domain.repository.WaterRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Offline-first sync: Room and the key-value stores stay the source of truth for the UI.
 * Pending changes are uploaded at start and shortly after each local change; failures stay pending.
 */
class CloudSyncManager(
    private val api: CaloriesApi,
    private val sessionStore: SessionStore,
    private val dao: ScanHistoryDao,
    private val profileRepository: UserProfileRepositoryImpl,
    private val waterRepository: WaterRepositoryImpl,
    private val pendingDeletions: PendingDeletionStore,
    private val syncTrigger: SyncTrigger,
    storeFactory: KeyValueStoreFactory
) {

    private val log = Logger.withTag("CloudSync")
    private val state = storeFactory.create(PREFS_NAME)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var started = false

    /** Signs in anonymously if needed, pulls server state, then uploads after every local change. */
    fun start() {
        if (started) return
        started = true
        scope.launch { syncNow(pullServerState = true) }
        scope.launch {
            syncTrigger.requests.collectLatest {
                delay(DEBOUNCE_MS)
                scope.launch { syncNow(pullServerState = false) }
            }
        }
    }

    private suspend fun syncNow(pullServerState: Boolean) = mutex.withLock {
        if (!ensureSession()) return@withLock
        step("profile") { syncProfile(pullServerState) }
        if (!ensureSession()) return@withLock
        step("deletions") { flushDeletions() }
        if (!ensureSession()) return@withLock
        step("meal logs") { pushMealLogs() }
        if (!ensureSession()) return@withLock
        step("water") { syncWater() }
    }

    private suspend fun step(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            log.i { "Sync '$name' postponed: offline (${e::class.simpleName})" }
        } catch (e: Exception) {
            log.w(e) { "Sync '$name' failed" }
        }
    }

    /** The session is re-checked before every step: a rejected token refresh clears it. */
    private suspend fun ensureSession(): Boolean {
        if (sessionStore.hasSession) return true
        return try {
            val response = api.authAnonymous(platformName)
            val auth = if (response.status.isSuccess()) response.body<AuthResponseDto>() else null
            val userId = auth?.userId
            val token = auth?.accessToken
            if (userId.isNullOrBlank() || token.isNullOrBlank()) {
                log.w { "Anonymous sign-in failed: HTTP ${response.status.value}" }
                return false
            }
            sessionStore.save(userId, token, auth.refreshToken)
            api.reloadTokens()
            onAccountSignedIn(userId)
            log.i { "Anonymous session created" }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.i { "Anonymous sign-in postponed: ${e::class.simpleName}" }
            false
        }
    }

    /** A different account owns nothing yet: everything is queued again; client ids prevent duplicates. */
    private suspend fun onAccountSignedIn(userId: String) {
        val previous = state.getString(KEY_SYNCED_USER_ID)
        if (previous != null && previous != userId) {
            dao.resetSyncState()
            profileRepository.markDirty()
            pendingDeletions.clear()
            log.i { "New account: local data queued for re-upload" }
        }
        state.edit { putString(KEY_SYNCED_USER_ID, userId) }
    }

    private suspend fun syncProfile(pull: Boolean) {
        if (profileRepository.isDirty) {
            val revision = profileRepository.revision
            val request = profileRepository.toRequest() ?: return
            val response = api.putProfile(request)
            if (response.status.isSuccess()) {
                profileRepository.onSynced(revision, response.body<ProfileResponseDto>().calculatedTargets)
            } else {
                log.w { "Profile upload failed: HTTP ${response.status.value}" }
            }
        } else if (pull) {
            val response = api.getProfile()
            if (response.status.isSuccess()) {
                profileRepository.applyServerTargets(response.body<ProfileResponseDto>().calculatedTargets)
            }
        }
    }

    private suspend fun flushDeletions() {
        pendingDeletions.all().forEach { remoteId ->
            val response = api.deleteMealLog(remoteId)
            if (response.status.isSuccess() || response.status == HttpStatusCode.NotFound) {
                pendingDeletions.remove(remoteId)
            } else {
                log.w { "Meal log delete failed: HTTP ${response.status.value}" }
            }
        }
    }

    private suspend fun pushMealLogs() {
        dao.getPendingSync().forEach { entity ->
            val request = entity.toMealLogRequest()
            val remoteId = entity.remoteId
            if (remoteId == null) {
                createMealLog(entity, request)
                return@forEach
            }
            val response = api.updateMealLog(remoteId, request)
            when {
                response.status.isSuccess() -> dao.markSynced(entity.id, remoteId, entity.updatedAt)
                response.status == HttpStatusCode.NotFound -> createMealLog(entity, request)
                else -> log.w { "Meal log update failed: HTTP ${response.status.value}" }
            }
        }
    }

    private suspend fun createMealLog(entity: ScanHistoryEntity, request: MealLogRequestDto) {
        val response = api.createMealLog(request)
        val remoteId = if (response.status.isSuccess()) response.body<MealLogResponseDto>().resolvedId else null
        if (remoteId.isNullOrBlank()) {
            log.w { "Meal log upload failed: HTTP ${response.status.value}" }
            return
        }
        if (dao.markSynced(entity.id, remoteId, entity.updatedAt) == 0) {
            dao.setRemoteId(entity.id, remoteId)
        }
    }

    private suspend fun syncWater() {
        for (event in waterRepository.pendingEvents()) {
            val response = api.createWaterLog(
                WaterLogRequestDto(
                    clientId = event.clientId,
                    amountMl = WaterRepository.ML_PER_GLASS,
                    loggedAt = event.loggedAt
                )
            )
            if (!response.status.isSuccess()) {
                log.w { "Water log upload failed: HTTP ${response.status.value}" }
                return
            }
            waterRepository.removePendingEvent(event.clientId)
        }
        val today = api.getWaterToday()
        if (today.status.isSuccess()) {
            val water = today.body<WaterTodayDto>()
            waterRepository.applyServerToday(water.date, water.totalGlasses)
        }
    }

    /** scanId is unique per entry and never changes, so it doubles as the idempotency key. */
    private fun ScanHistoryEntity.toMealLogRequest(): MealLogRequestDto {
        val factor = consumedFactor
        return MealLogRequestDto(
            clientId = scanId,
            clientLogId = scanId,
            source = resolvedLogSource,
            referenceId = referenceId ?: scanId.takeIf { resolvedLogSource == ScanHistoryEntity.SOURCE_SCAN },
            mealName = mealName,
            mealType = HistoryMapper.mealTypeOf(this).key,
            consumedWeightG = consumedWeightG ?: servingWeightG,
            nutrition = BasicNutritionDto(
                energyKcal = consumedEnergyKcal,
                proteinG = consumedProteinG,
                carbohydrateG = consumedCarbohydrateG,
                fatG = consumedFatG,
                sugarG = totalSugarG?.times(factor),
                sodiumMg = totalSodiumMg?.times(factor)
            ),
            loggedAt = timestamp
        )
    }

    private companion object {
        const val DEBOUNCE_MS = 1_500L
        const val PREFS_NAME = "cloud_sync_state"
        const val KEY_SYNCED_USER_ID = "synced_user_id"
    }
}