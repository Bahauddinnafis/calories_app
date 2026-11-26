package com.nafis.calories.di

import androidx.room.RoomDatabase
import com.nafis.calories.core.config.AppConfig
import com.nafis.calories.core.network.HttpClientFactory
import com.nafis.calories.data.local.AppDatabase
import com.nafis.calories.data.local.buildAppDatabase
import com.nafis.calories.data.remote.api.CaloriesApi
import com.nafis.calories.data.remote.session.SessionStore
import com.nafis.calories.data.repository.FoodAnalysisRepositoryImpl
import com.nafis.calories.data.repository.InsightRepositoryImpl
import com.nafis.calories.data.repository.MealRecommendationRepositoryImpl
import com.nafis.calories.data.repository.PackagedProductRepositoryImpl
import com.nafis.calories.data.repository.ScanHistoryRepositoryImpl
import com.nafis.calories.data.repository.UserProfileRepositoryImpl
import com.nafis.calories.data.repository.WaterRepositoryImpl
import com.nafis.calories.data.sync.CloudSyncManager
import com.nafis.calories.data.sync.PendingDeletionStore
import com.nafis.calories.data.sync.SyncTrigger
import com.nafis.calories.domain.repository.FoodAnalysisRepository
import com.nafis.calories.domain.repository.InsightRepository
import com.nafis.calories.domain.repository.MealRecommendationRepository
import com.nafis.calories.domain.repository.PackagedProductRepository
import com.nafis.calories.domain.repository.ScanHistoryRepository
import com.nafis.calories.domain.repository.UserProfileRepository
import com.nafis.calories.domain.repository.WaterRepository
import com.nafis.calories.domain.usecase.GetScanDailyContextUseCase
import com.nafis.calories.domain.usecase.LogMealUseCase
import com.nafis.calories.ui.history.HistoryViewModel
import com.nafis.calories.ui.profile.ProfileViewModel
import com.nafis.calories.ui.scan.HomeViewModel
import com.nafis.calories.ui.scan.ScanViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import kotlin.time.Clock

fun initKoin(config: AppConfig, appDeclaration: KoinAppDeclaration = {}) {
    val koin = startKoin {
        appDeclaration()
        modules(coreModule(config), platformModule, dataModule, syncModule, domainModule, viewModelModule)
    }.koin
    koin.get<CloudSyncManager>().start()
}

private fun coreModule(config: AppConfig) = module {
    single { config }
    single<Clock> { Clock.System }
    singleOf(::SessionStore)
    single { HttpClientFactory.create(engine = get(), config = get(), sessionStore = get()) }
    singleOf(::CaloriesApi)
}

private val dataModule = module {
    single { get<RoomDatabase.Builder<AppDatabase>>().buildAppDatabase() }
    single { get<AppDatabase>().scanHistoryDao() }

    singleOf(::ScanHistoryRepositoryImpl) { bind<ScanHistoryRepository>() }
    singleOf(::UserProfileRepositoryImpl) { bind<UserProfileRepository>() }
    singleOf(::WaterRepositoryImpl) { bind<WaterRepository>() }
    singleOf(::FoodAnalysisRepositoryImpl) { bind<FoodAnalysisRepository>() }
    singleOf(::PackagedProductRepositoryImpl) { bind<PackagedProductRepository>() }
    singleOf(::MealRecommendationRepositoryImpl) { bind<MealRecommendationRepository>() }
    singleOf(::InsightRepositoryImpl) { bind<InsightRepository>() }
}

private val syncModule = module {
    singleOf(::SyncTrigger)
    singleOf(::PendingDeletionStore)
    singleOf(::CloudSyncManager)
}

private val domainModule = module {
    factoryOf(::LogMealUseCase)
    factoryOf(::GetScanDailyContextUseCase)
}

private val viewModelModule = module {
    viewModelOf(::ScanViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::ProfileViewModel)
}

internal expect val platformModule: Module