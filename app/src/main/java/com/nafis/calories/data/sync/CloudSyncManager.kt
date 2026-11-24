package com.nafis.calories.data.sync

import android.content.Context
import android.util.Log
import com.nafis.calories.data.local.AppDatabase
import com.nafis.calories.data.local.entity.ScanHistoryEntity
import com.nafis.calories.data.local.mapper.HistoryMapper
import com.nafis.calories.data.remote.api.RetrofitClient
import com.nafis.calories.data.remote.dto.BasicNutritionDto
import com.nafis.calories.data.remote.dto.MealLogRequestDto
import com.nafis.calories.data.remote.dto.WaterLogRequestDto
import com.nafis.calories.data.remote.session.SessionStore
import com.nafis.calories.data.repository.UserProfileRepository
import com.nafis.calories.data.repository.WaterTrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/**
 * Offline-first cloud sync. Room and SharedPreferences stay the source of truth for the UI; this
 * uploads pending changes (profile, meal logs, deletions, water events) whenever possible:
 * at app start and shortly after each local change. Failures leave items pending for the next run.
 */
object CloudSyncManager {

    private const val TAG = "CloudSync"
    private const val DEBOUNCE_MS = 1_500L
    private const val PREFS_NAME = "cloud_sync_state"
    private const val KEY_SYNCED_USER_ID = "synced_user_id"

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var debounceJob: Job? = null

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        SessionStore.init(appContext)
    }

    /** First sync at app start: signs in anonymously if needed and pulls server state. */
    fun bootstrap() {
        if (!::appContext.isInitialized) return
        scope.launch { syncNow(pullServerState = true) }
    }

    /** Schedules an upload shortly after a local change; bursts of changes share one run. */
    fun requestSync() {
        if (!::appContext.isInitialized) return
        synchronized(this) {
            debounceJob?.cancel()
            // Only the wait is cancellable; a sync that already started runs to completion.
            debounceJob = scope.launch {
                delay(DEBOUNCE_MS)
                scope.launch { syncNow(pullServerState = false) }
            }
        }
    }

    private suspend fun syncNow(pullServerState: Boolean) = mutex.withLock {
        // The session is re-checked before every push: TokenAuthenticator clears it when a
        // refresh is rejected, and the next step then signs in anonymously again.
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
        } catch (e: IOException) {
            Log.i(TAG, "Sync '$name' postponed: offline (${e.javaClass.simpleName})")
        } catch (e: Exception) {
            Log.w(TAG, "Sync '$name' failed", e)
        }
    }

    /** Signs in anonymously when there is no access token (first launch or after a rejected refresh). */
    private suspend fun ensureSession(): Boolean {
        if (!SessionStore.accessToken.isNullOrBlank()) return true
        return try {
            val response = RetrofitClient.api.authAnonymous()
            val body = response.body()
            val userId = body?.userId
            val token = body?.accessToken
            if (response.isSuccessful && !userId.isNullOrBlank() && !token.isNullOrBlank()) {
                SessionStore.save(userId, token, body.refreshToken)
                onAccountSignedIn(userId)
                Log.i(TAG, "Anonymous session created")
                true
            } else {
                Log.w(TAG, "Anonymous sign-in failed: HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.i(TAG, "Anonymous sign-in postponed: ${e.javaClass.simpleName}")
            false
        }
    }

    /**
     * A different account means nothing on the server is ours yet (e.g. in-memory storage was reset):
     * mark everything for re-upload. Client IDs keep the re-upload free of duplicates.
     */
    private suspend fun onAccountSignedIn(userId: String) {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val previous = prefs.getString(KEY_SYNCED_USER_ID, null)
        if (previous != null && previous != userId) {
            AppDatabase.getDatabase(appContext).scanHistoryDao().resetSyncState()
            UserProfileRepository.getInstance(appContext).markDirty()
            PendingDeletionStore.clear(appContext)
            Log.i(TAG, "New account: local data queued for re-upload")
        }
        prefs.edit().putString(KEY_SYNCED_USER_ID, userId).apply()
    }

    private suspend fun syncProfile(pull: Boolean) {
        val repository = UserProfileRepository.getInstance(appContext)
        if (repository.isDirty) {
            val revision = repository.revision
            val request = repository.toRequest() ?: return
            val response = RetrofitClient.api.putProfile(request)
            if (response.isSuccessful) {
                repository.onSynced(revision, response.body()?.calculatedTargets)
            } else {
                Log.w(TAG, "Profile upload failed: HTTP ${response.code()}")
            }
        } else if (pull) {
            val response = RetrofitClient.api.getProfile()
            if (response.isSuccessful) repository.applyServerTargets(response.body()?.calculatedTargets)
        }
    }

    private suspend fun flushDeletions() {
        PendingDeletionStore.all(appContext).forEach { remoteId ->
            val response = RetrofitClient.api.deleteMealLog(remoteId)
            // 404: already gone on the server, which is the goal.
            if (response.isSuccessful || response.code() == 404) {
                PendingDeletionStore.remove(appContext, remoteId)
            } else {
                Log.w(TAG, "Meal log delete failed: HTTP ${response.code()}")
            }
        }
    }

    private suspend fun pushMealLogs() {
        val dao = AppDatabase.getDatabase(appContext).scanHistoryDao()
        dao.getPendingSync().forEach { entity ->
            val request = entity.toMealLogRequest()
            val existingRemoteId = entity.remoteId
            if (existingRemoteId != null) {
                val response = RetrofitClient.api.updateMealLog(existingRemoteId, request)
                when {
                    response.isSuccessful -> dao.markSynced(entity.id, existingRemoteId, entity.updatedAt)
                    // Deleted on the server: upload it again as a new log.
                    response.code() == 404 -> createMealLog(entity, request)
                    else -> Log.w(TAG, "Meal log update failed: HTTP ${response.code()}")
                }
            } else {
                createMealLog(entity, request)
            }
        }
    }

    private suspend fun createMealLog(entity: ScanHistoryEntity, request: MealLogRequestDto) {
        val dao = AppDatabase.getDatabase(appContext).scanHistoryDao()
        // client_log_id makes this idempotent, so a retried upload isn't counted twice.
        val response = RetrofitClient.api.createMealLog(request)
        val remoteId = response.body()?.resolvedId
        if (!response.isSuccessful || remoteId.isNullOrBlank()) {
            Log.w(TAG, "Meal log upload failed: HTTP ${response.code()}")
            return
        }
        if (dao.markSynced(entity.id, remoteId, entity.updatedAt) == 0) {
            // Edited during the upload: keep it pending so the next run PATCHes the newer values.
            dao.setRemoteId(entity.id, remoteId)
        }
    }

    private suspend fun syncWater() {
        val repository = WaterTrackerRepository.getInstance(appContext)
        for (event in repository.pendingEvents()) {
            val response = RetrofitClient.api.createWaterLog(
                WaterLogRequestDto(
                    clientId = event.clientId,
                    amountMl = WaterTrackerRepository.ML_PER_GLASS,
                    loggedAt = event.loggedAt
                )
            )
            if (!response.isSuccessful) {
                Log.w(TAG, "Water log upload failed: HTTP ${response.code()}")
                return
            }
            repository.removePendingEvent(event.clientId)
        }
        val today = RetrofitClient.api.getWaterToday()
        if (today.isSuccessful) {
            today.body()?.let { repository.applyServerToday(it.date, it.totalGlasses) }
        }
    }

    private fun ScanHistoryEntity.toMealLogRequest(): MealLogRequestDto {
        val factor = consumedFactor
        // scanId is unique per local entry and never changes, so it doubles as the idempotency key.
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
}
