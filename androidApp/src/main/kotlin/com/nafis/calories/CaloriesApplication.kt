package com.nafis.calories

import android.app.Application
import com.nafis.calories.core.config.AppConfig
import com.nafis.calories.di.initKoin
import org.koin.android.ext.koin.androidContext

class CaloriesApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin(AppConfig.create(BuildConfig.BACKEND_BASE_URL, BuildConfig.DEBUG)) {
            androidContext(this@CaloriesApplication)
        }
    }
}
