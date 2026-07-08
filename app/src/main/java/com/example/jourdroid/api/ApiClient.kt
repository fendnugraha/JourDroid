package com.example.jourdroid.api

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
//    private const val BASE_URL = "http://10.0.2.2:8000/" // IP Khusus Emulator Android untuk panggil localhost laptopmu!
    private const val BASE_URL = " https://earmark-crept-qualifier.ngrok-free.dev/"
    fun getApiService(context: Context): ApiService {

        // Membuka SharedPreferences untuk mengambil token yang tersimpan
        val sharedPref = context.getSharedPreferences("AUTH_PREF", Context.MODE_PRIVATE)
        val token = sharedPref.getString("BEARER_TOKEN", "")

        // Ini adalah INTERCEPTOR (Otomatis menyelipkan Bearer Token ke Header)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                if (!token.isNullOrEmpty()) {
                    // Menyisipkan token secara otomatis seperti di Postman / Axios interceptor
                    request.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(request.build())
            }.build()

        // Build Retrofit-nya
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Pasang client yang sudah bertoken tadi
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}