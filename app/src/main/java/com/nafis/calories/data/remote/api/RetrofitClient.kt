package com.nafis.calories.data.remote.api

import com.nafis.calories.BuildConfig
import com.nafis.calories.data.remote.auth.AuthInterceptor
import com.nafis.calories.data.remote.auth.AuthRefreshApi
import com.nafis.calories.data.remote.auth.TokenAuthenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            // HEADERS level: Logs request/response headers without multipart image bodies
            HttpLoggingInterceptor.Level.HEADERS
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        // Never print the bearer token to logcat.
        redactHeader(AuthInterceptor.HEADER_AUTHORIZATION)
    }

    private val refreshOkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(withAuthorization = false))
        .addInterceptor(loggingInterceptor())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val authRefreshApi: AuthRefreshApi by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(refreshOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthRefreshApi::class.java)
    }

    private val mainOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor())
            .authenticator(TokenAuthenticator(authRefreshApi))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(mainOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: CaloriesBackendApi by lazy {
        retrofit.create(CaloriesBackendApi::class.java)
    }
}