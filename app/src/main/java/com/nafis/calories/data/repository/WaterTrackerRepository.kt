package com.nafis.calories.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nafis.calories.data.sync.CloudSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Today's water intake, kept on the device and uploaded as individual +250 ml events
 * (`POST /api/v1/me/water-logs`). The day starts at 0 glasses.
 */
data class WaterEvent(val clientId: String, val loggedAt: Long)

class WaterTrackerRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _waterGlasses = MutableStateFlow(readCurrentGlasses())
    val waterGlasses: StateFlow<Int> = _waterGlasses.asStateFlow()

    @Synchronized
    fun incrementWater(amountGlasses: Int = 1) {
        val current = readCurrentGlasses()
        val updated = (current + amountGlasses).coerceIn(0, MAX_GLASSES)
        val added = updated - current
        if (added <= 0) return

        val now = System.currentTimeMillis()
        val events = pendingEvents() + List(added) { WaterEvent(UUID.randomUUID().toString(), now) }
        prefs.edit {
            putString(KEY_LAST_DATE, getTodayKey())
            putInt(KEY_GLASSES, updated)
            putString(KEY_PENDING_EVENTS, encode(events))
        }
        _waterGlasses.value = updated
        CloudSyncManager.requestSync()
    }

    /** +1 glass events not yet uploaded; [WaterEvent.clientId] makes re-uploads idempotent. */
    @Synchronized
    fun pendingEvents(): List<WaterEvent> {
        val raw = prefs.getString(KEY_PENDING_EVENTS, null)?.takeIf { it.isNotBlank() } ?: return emptyList()
        val entries = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val events = entries.mapIndexedNotNull { index, entry ->
            val separator = entry.indexOf(ID_SEPARATOR)
            if (separator > 0) {
                entry.substring(separator + 1).toLongOrNull()?.let { WaterEvent(entry.substring(0, separator), it) }
            } else {
                // Pre-UUID entry (timestamp only): give it a stable id derived from its position.
                entry.toLongOrNull()?.let { WaterEvent("water-$it-$index", it) }
            }
        }
        if (entries.any { ID_SEPARATOR !in it }) prefs.edit { putString(KEY_PENDING_EVENTS, encode(events)) }
        return events
    }

    @Synchronized
    fun removePendingEvent(clientId: String) {
        val remaining = pendingEvents().filterNot { it.clientId == clientId }
        prefs.edit { putString(KEY_PENDING_EVENTS, encode(remaining)) }
    }

    @Synchronized
    fun applyServerToday(date: String?, totalGlasses: Int?) {
        if (totalGlasses == null || pendingEvents().isNotEmpty()) return
        if (date != null && date != getTodayKey()) return
        val glasses = maxOf(readCurrentGlasses(), totalGlasses.coerceIn(0, MAX_GLASSES))
        if (glasses == readCurrentGlasses()) return
        prefs.edit {
            putString(KEY_LAST_DATE, getTodayKey())
            putInt(KEY_GLASSES, glasses)
        }
        _waterGlasses.value = glasses
    }

    /** Re-reads the count, e.g. when the app returns to the foreground on a new day. */
    fun refresh() {
        _waterGlasses.value = readCurrentGlasses()
    }

    @Synchronized
    private fun readCurrentGlasses(): Int {
        val todayKey = getTodayKey()
        return if (prefs.getString(KEY_LAST_DATE, null) == todayKey) {
            prefs.getInt(KEY_GLASSES, 0)
        } else {
            // New day: start from zero. Pending events from earlier days are still uploaded.
            prefs.edit {
                putString(KEY_LAST_DATE, todayKey)
                putInt(KEY_GLASSES, 0)
            }
            0
        }
    }

    private fun encode(events: List<WaterEvent>): String =
        events.joinToString(",") { "${it.clientId}$ID_SEPARATOR${it.loggedAt}" }

    private fun getTodayKey(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    companion object {
        private const val PREFS_NAME = "water_tracker"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_GLASSES = "glasses_consumed"
        private const val KEY_PENDING_EVENTS = "pending_events"
        private const val ID_SEPARATOR = '@'
        const val ML_PER_GLASS = 250
        const val MAX_GLASSES = 20

        @Volatile
        private var instance: WaterTrackerRepository? = null

        fun getInstance(context: Context): WaterTrackerRepository =
            instance ?: synchronized(this) {
                instance ?: WaterTrackerRepository(context).also { instance = it }
            }
    }
}