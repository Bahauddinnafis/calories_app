package com.nafis.calories.data.remote.auth

import com.nafis.calories.data.remote.session.SessionStore
import okhttp3.Interceptor
import okhttp3.Response
import java.util.TimeZone

/**
 * Adds `Accept`, `X-Timezone`, and (when signed in) `Authorization: Bearer <token>` to every call.
 * [withAuthorization] = false is used by the refresh client, which must never send the old token.
 */
class AuthInterceptor(private val withAuthorization: Boolean = true) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("Accept", "application/json")
            .header("X-Timezone", TimeZone.getDefault().id)
        if (withAuthorization && chain.request().header(HEADER_AUTHORIZATION) == null) {
            SessionStore.accessToken?.takeIf { it.isNotBlank() }?.let {
                builder.header(HEADER_AUTHORIZATION, bearer(it))
            }
        }
        return chain.proceed(builder.build())
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
        fun bearer(token: String) = "Bearer $token"
    }
}