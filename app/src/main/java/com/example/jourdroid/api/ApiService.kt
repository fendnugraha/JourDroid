package com.example.jourdroid.api

import com.example.jourdroid.data.LoginResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    // 1. Endpoint untuk Login (Tidak butuh token)
    @FormUrlEncoded
    @POST("api/android/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    // 2. Endpoint yang butuh Bearer Token (Proteksi Sanctum)
    @GET("api/user-profile")
    suspend fun getUserProfile(): LoginResponse

    // logout
    @POST("api/logout")
    suspend fun logout(): retrofit2.Response<Unit>

}