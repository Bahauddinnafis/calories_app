package com.nafis.calories.di

import com.nafis.calories.core.storage.KeyValueStoreFactory
import com.nafis.calories.core.storage.KeystoreSecureStorage
import com.nafis.calories.core.storage.SecureStorage
import com.nafis.calories.core.storage.SharedPreferencesStoreFactory
import com.nafis.calories.data.local.databaseBuilder
import com.nafis.calories.data.local.image.ImageStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<KeyValueStoreFactory> { SharedPreferencesStoreFactory(androidContext()) }
    single<SecureStorage> { KeystoreSecureStorage(androidContext()) }
    single<HttpClientEngine> { OkHttp.create() }
    single { databaseBuilder(androidContext()) }
    single { ImageStore(baseDir = androidContext().filesDir.absolutePath, clock = get()) }
}