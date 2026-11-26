package com.nafis.calories.di

import com.nafis.calories.core.storage.KeychainSecureStorage
import com.nafis.calories.core.storage.KeyValueStoreFactory
import com.nafis.calories.core.storage.SecureStorage
import com.nafis.calories.core.storage.UserDefaultsStoreFactory
import com.nafis.calories.data.local.databaseBuilder
import com.nafis.calories.data.local.documentDirectory
import com.nafis.calories.data.local.image.ImageStore
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<KeyValueStoreFactory> { UserDefaultsStoreFactory() }
    single<SecureStorage> { KeychainSecureStorage(service = "com.nafis.calories.session") }
    single<HttpClientEngine> { Darwin.create() }
    single { databaseBuilder() }
    single { ImageStore(baseDir = documentDirectory(), clock = get()) }
}