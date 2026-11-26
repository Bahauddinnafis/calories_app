package com.nafis.calories.core.network

import com.nafis.calories.core.config.AppConfig
import com.nafis.calories.core.serialization.AppJson
import com.nafis.calories.data.remote.dto.AuthResponseDto
import com.nafis.calories.data.remote.dto.RefreshTokenRequestDto
import com.nafis.calories.data.remote.session.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.TimeZone
import kotlinx.io.IOException

object HttpClientFactory {

    private const val REFRESH_PATH = "api/v1/auth/refresh"

    fun create(engine: HttpClientEngine, config: AppConfig, sessionStore: SessionStore): HttpClient =
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(AppJson)
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
            install(Logging) {
                level = if (config.isDebug) LogLevel.HEADERS else LogLevel.NONE
                sanitizeHeader { it == HttpHeaders.Authorization }
            }
            install(Auth) {
                bearer {
                    loadTokens { sessionStore.bearerTokens() }
                    refreshTokens { refresh(sessionStore) }
                    sendWithoutRequest { true }
                }
            }
            defaultRequest {
                url(config.baseUrl)
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header("X-Timezone", TimeZone.currentSystemDefault().id)
            }
        }

    private fun SessionStore.bearerTokens(): BearerTokens? =
        accessToken?.takeIf { it.isNotBlank() }?.let { BearerTokens(it, refreshToken) }

    /**
     * Runs once per expiry: Ktor queues concurrent 401s behind this call.
     * Offline keeps the session for a later retry; a rejected refresh clears it.
     */
    private suspend fun RefreshTokensParams.refresh(sessionStore: SessionStore): BearerTokens? {
        val current = sessionStore.accessToken
        if (!current.isNullOrBlank() && current != oldTokens?.accessToken) {
            return BearerTokens(current, sessionStore.refreshToken)
        }

        val refreshToken = sessionStore.refreshToken
        if (refreshToken.isNullOrBlank()) {
            sessionStore.clearSession()
            return null
        }

        val response = try {
            client.post(REFRESH_PATH) {
                markAsRefreshTokenRequest()
                contentType(ContentType.Application.Json)
                setBody(RefreshTokenRequestDto(refreshToken))
            }
        } catch (e: IOException) {
            return null
        }

        val tokens = if (response.status.isSuccess()) response.body<AuthResponseDto>() else null
        val newAccess = tokens?.accessToken
        if (newAccess.isNullOrBlank()) {
            sessionStore.clearSession()
            return null
        }

        sessionStore.updateTokens(newAccess, tokens.refreshToken)
        return BearerTokens(newAccess, tokens.refreshToken ?: refreshToken)
    }
}