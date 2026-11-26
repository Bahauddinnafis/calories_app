package com.nafis.calories

import androidx.compose.ui.window.ComposeUIViewController
import com.nafis.calories.core.config.AppConfig
import com.nafis.calories.di.initKoin
import platform.UIKit.UIViewController

/** Called once from the Swift app entry point before any UI is shown. */
fun configureApp(baseUrl: String, isDebug: Boolean) {
    initKoin(AppConfig.create(baseUrl, isDebug))
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    CaloriesAppScreen()
}
