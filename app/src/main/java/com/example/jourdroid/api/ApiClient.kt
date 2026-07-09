package com.example.jourdroid.api

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

import com.example.jourdroid.utils.AuthManager

object ApiClient {
    private const val BASE_URL = "https://api.three-komunika.com/"

    private var cachedService: ApiService? = null

    private fun getOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authManager = AuthManager(context)

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val token = authManager.getToken()
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    fun getApiService(context: Context): ApiService {
        if (cachedService == null) {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient(context))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            cachedService = retrofit.create(ApiService::class.java)
        }
        return cachedService!!
    }
}
