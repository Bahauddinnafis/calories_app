package com.nafis.calories

import android.app.Application
import com.nafis.calories.data.sync.CloudSyncManager

class CaloriesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Sets up the secure session store, then signs in anonymously (first launch) and syncs.
        CloudSyncManager.init(this)
        CloudSyncManager.bootstrap()
    }
}
