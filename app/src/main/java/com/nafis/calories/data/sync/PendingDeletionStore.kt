package com.nafis.calories.data.sync

import android.content.Context
import androidx.core.content.edit

/** Backend meal-log IDs deleted on the device whose `DELETE` hasn't reached the server yet. */
object PendingDeletionStore {

    private const val PREFS_NAME = "cloud_sync_queue"
    private const val KEY_DELETED_REMOTE_IDS = "deleted_meal_log_ids"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun add(context: Context, remoteIds: Collection<String>) {
        if (remoteIds.isEmpty()) return
        val current = all(context)
        prefs(context).edit { putStringSet(KEY_DELETED_REMOTE_IDS, current + remoteIds) }
    }

    @Synchronized
    fun all(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_DELETED_REMOTE_IDS, null)?.toSet().orEmpty()

    @Synchronized
    fun clear(context: Context) {
        prefs(context).edit { remove(KEY_DELETED_REMOTE_IDS) }
    }

    @Synchronized
    fun remove(context: Context, remoteId: String) {
        prefs(context).edit { putStringSet(KEY_DELETED_REMOTE_IDS, all(context) - remoteId) }
    }
}