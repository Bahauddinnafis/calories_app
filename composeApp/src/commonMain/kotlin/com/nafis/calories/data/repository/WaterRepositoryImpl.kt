package com.nafis.calories.data.repository

import com.nafis.calories.core.storage.KeyValueStoreFactory
import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.domain.repository.WaterRepository
import com.nafis.calories.domain.repository.WaterRepository.Companion.MAX_GLASSES
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.uuid.Uuid

data class WaterEvent(val clientId: String, val loggedAt: Long)

/** Today's glasses, kept on the device and uploaded as individual +250 ml events. */
class WaterRepositoryImpl(
    storeFactory: KeyValueStoreFactory,
    private val clock: Clock,
    private val syncTrigger: SyncTrigger
) : WaterRepository {

    private val store = storeFactory.create(PREFS_NAME)
    private val lock = SynchronizedObject()

    private val _waterGlasses = MutableStateFlow(readCurrentGlasses())
    override val waterGlasses: StateFlow<Int> = _waterGlasses.asStateFlow()

    override fun addGlasses(amount: Int) = synchronized(lock) {
        val current = readCurrentGlasses()
        val updated = (current + amount).coerceIn(0, MAX_GLASSES)
        val added = updated - current
        if (added <= 0) return@synchronized

        val now = clock.now().toEpochMilliseconds()
        val events = pendingEvents() + List(added) { WaterEvent(Uuid.random().toString(), now) }
        store.edit {
            putString(KEY_LAST_DATE, todayKey())
            putInt(KEY_GLASSES, updated)
            putString(KEY_PENDING_EVENTS, encode(events))
        }
        _waterGlasses.value = updated
        syncTrigger.requestSync()
    }

    /** Events not uploaded yet; [WaterEvent.clientId] makes re-uploads idempotent. */
    fun pendingEvents(): List<WaterEvent> = synchronized(lock) {
        val raw = store.getString(KEY_PENDING_EVENTS)?.takeIf { it.isNotBlank() } ?: return@synchronized emptyList()
        val entries = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val events = entries.mapIndexedNotNull { index, entry ->
            val separator = entry.indexOf(ID_SEPARATOR)
            if (separator > 0) {
                entry.substring(separator + 1).toLongOrNull()?.let { WaterEvent(entry.substring(0, separator), it) }
            } else {
                entry.toLongOrNull()?.let { WaterEvent("water-$it-$index", it) }
            }
        }
        if (entries.any { ID_SEPARATOR !in it }) store.edit { putString(KEY_PENDING_EVENTS, encode(events)) }
        events
    }

    fun removePendingEvent(clientId: String) = synchronized(lock) {
        val remaining = pendingEvents().filterNot { it.clientId == clientId }
        store.edit { putString(KEY_PENDING_EVENTS, encode(remaining)) }
    }

    fun applyServerToday(date: String?, totalGlasses: Int?) = synchronized(lock) {
        if (totalGlasses == null || pendingEvents().isNotEmpty()) return@synchronized
        val today = todayKey()
        if (date != null && date != today) return@synchronized
        val current = readCurrentGlasses()
        val glasses = maxOf(current, totalGlasses.coerceIn(0, MAX_GLASSES))
        if (glasses == current) return@synchronized
        store.edit {
            putString(KEY_LAST_DATE, today)
            putInt(KEY_GLASSES, glasses)
        }
        _waterGlasses.value = glasses
    }

    /** Re-reads the count, e.g. when the app returns to the foreground on a new day. */
    override fun refresh() {
        _waterGlasses.value = synchronized(lock) { readCurrentGlasses() }
    }

    private fun readCurrentGlasses(): Int {
        val today = todayKey()
        if (store.getString(KEY_LAST_DATE) == today) return store.getInt(KEY_GLASSES, 0)
        store.edit {
            putString(KEY_LAST_DATE, today)
            putInt(KEY_GLASSES, 0)
        }
        return 0
    }

    private fun encode(events: List<WaterEvent>): String =
        events.joinToString(",") { "${it.clientId}$ID_SEPARATOR${it.loggedAt}" }

    private fun todayKey(): String = clock.todayIn(TimeZone.currentSystemDefault()).toString()

    private companion object {
        const val PREFS_NAME = "water_tracker"
        const val KEY_LAST_DATE = "last_date"
        const val KEY_GLASSES = "glasses_consumed"
        const val KEY_PENDING_EVENTS = "pending_events"
        const val ID_SEPARATOR = '@'
    }
}
