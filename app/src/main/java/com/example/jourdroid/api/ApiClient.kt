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
    private const val BASE_URL = "https://sandbox.three-komunika.com/"

    private var cachedService: ApiService? = null

    private fun getOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS // 🟢 Reduced logging level to improve performance
        }
        
        val authManager = AuthManager(context.applicationContext)

        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = authManager.getToken()
                Log.d("ApiClient", "Using token: $token")
                val requestBuilder = chain.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Requested-With", "XMLHttpRequest")
                
                // 🟢 SKIP Token on login & test endpoints to prevent 401 if token is expired/invalid
                val path = chain.request().url.encodedPath
                val skipToken = path.contains("login") || path.contains("test-connection")
                
                if (!token.isNullOrEmpty() && !skipToken) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    fun getApiService(context: Context): ApiService {
        if (cachedService == null) {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient(context.applicationContext))
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            cachedService = retrofit.create(ApiService::class.java)
        }
        return cachedService!!
    }
}
