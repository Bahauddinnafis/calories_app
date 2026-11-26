package com.nafis.calories.data.sync

import com.nafis.calories.core.storage.KeyValueStoreFactory
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/** Remote meal-log ids deleted locally that still have to be deleted on the backend. */
class PendingDeletionStore(storeFactory: KeyValueStoreFactory) {

    private val store = storeFactory.create(PREFS_NAME)
    private val lock = SynchronizedObject()

    fun all(): Set<String> = synchronized(lock) { store.getStringSet(KEY_DELETED_REMOTE_IDS).orEmpty() }

    fun add(remoteIds: Collection<String>) = synchronized(lock) {
        if (remoteIds.isEmpty()) return@synchronized
        store.edit { putStringSet(KEY_DELETED_REMOTE_IDS, all() + remoteIds) }
    }

    fun remove(remoteId: String) = synchronized(lock) {
        store.edit { putStringSet(KEY_DELETED_REMOTE_IDS, all() - remoteId) }
    }

    fun clear() = synchronized(lock) {
        store.edit { remove(KEY_DELETED_REMOTE_IDS) }
    }

    private companion object {
        const val PREFS_NAME = "cloud_sync_queue"
        const val KEY_DELETED_REMOTE_IDS = "deleted_meal_log_ids"
    }
}
