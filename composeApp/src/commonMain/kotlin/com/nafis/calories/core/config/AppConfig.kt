package com.nafis.calories.core.config

data class AppConfig(
    val baseUrl: String,
    val isDebug: Boolean
) {
    companion object {
        fun create(rawBaseUrl: String, isDebug: Boolean): AppConfig {
            val trimmed = rawBaseUrl.trim()
            val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed
            } else {
                "https://$trimmed"
            }
            return AppConfig(baseUrl = withScheme.trimEnd('/') + "/", isDebug = isDebug)
        }
    }
}
