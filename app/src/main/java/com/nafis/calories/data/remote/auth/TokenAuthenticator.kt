package com.nafis.calories.data.remote.auth

import android.util.Log
import com.nafis.calories.data.remote.dto.RefreshTokenRequestDto
import com.nafis.calories.data.remote.session.SessionStore
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/**
 * Handles `401 Unauthorized` by refreshing the access token once and retrying the request.
 *
 * Concurrent 401s are serialised on [lock]; whoever enters after a successful refresh sees a new
 * token and just retries with it, so `POST /auth/refresh` runs only once per expiry.
 * If the refresh is rejected, the session is cleared and the sync re-authenticates anonymously.
 */
class TokenAuthenticator(private val refreshApi: AuthRefreshApi) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val failedHeader = response.request.header(AuthInterceptor.HEADER_AUTHORIZATION) ?: return null
        if (responseCount(response) >= MAX_ATTEMPTS) return null
        val failedToken = failedHeader.removePrefix("Bearer ").trim()

        synchronized(lock) {
            // Double-check: another request may already have refreshed while this one waited.
            val current = SessionStore.accessToken
            if (!current.isNullOrBlank() && current != failedToken) {
                return response.request.withToken(current)
            }

            val refreshToken = SessionStore.refreshToken
            if (refreshToken.isNullOrBlank()) {
                SessionStore.clearSession()
                return null
            }

            val refreshResponse = try {
                refreshApi.refresh(RefreshTokenRequestDto(refreshToken)).execute()
            } catch (e: IOException) {
                // Offline: keep the session and let the caller retry later.
                Log.i(TAG, "Token refresh postponed: ${e.javaClass.simpleName}")
                return null
            }

            val newAccess = refreshResponse.body()?.accessToken
            if (refreshResponse.isSuccessful && !newAccess.isNullOrBlank()) {
                SessionStore.updateTokens(newAccess, refreshResponse.body()?.refreshToken)
                return response.request.withToken(newAccess)
            }

            Log.w(TAG, "Token refresh rejected (HTTP ${refreshResponse.code()}); clearing session")
            SessionStore.clearSession()
            return null
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AuthInterceptor.HEADER_AUTHORIZATION, AuthInterceptor.bearer(token)).build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val TAG = "TokenAuthenticator"
        const val MAX_ATTEMPTS = 2
    }
}