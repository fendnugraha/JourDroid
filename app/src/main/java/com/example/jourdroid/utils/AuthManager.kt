package com.example.jourdroid.utils

import android.content.Context
import androidx.core.content.edit

class AuthManager(context: Context) {
    private val sharedPref = context.getSharedPreferences("AUTH_PREF", Context.MODE_PRIVATE)

    // Fungsi menyimpan token setelah sukses login
    fun saveToken(token: String) {
        sharedPref.edit { putString("BEARER_TOKEN", token) }
    }

    // Fungsi mengambil token
    fun getToken(): String? {
        return sharedPref.getString("BEARER_TOKEN", null)
    }

    // Fungsi hapus token (saat user klik logout)
    fun clearAuth() {
        sharedPref.edit { clear() }
    }
}