package com.nafis.calories.data.remote.auth

import com.nafis.calories.data.remote.dto.AuthResponseDto
import com.nafis.calories.data.remote.dto.RefreshTokenRequestDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Blocking API used from [TokenAuthenticator], served by a client without the authenticator. */
interface AuthRefreshApi {

    @POST("api/v1/auth/refresh")
    fun refresh(@Body body: RefreshTokenRequestDto): Call<AuthResponseDto>
}