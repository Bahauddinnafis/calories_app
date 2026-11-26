package com.nafis.calories.data.sync

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Repositories signal local changes here; [CloudSyncManager] uploads them shortly after. */
class SyncTrigger {

    private val _requests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val requests: SharedFlow<Unit> = _requests.asSharedFlow()

    fun requestSync() {
        _requests.tryEmit(Unit)
    }
}
