package com.example.jourdroid.utils

import android.content.Context
import com.example.jourdroid.data.UserData
import com.google.gson.Gson
import androidx.core.content.edit

class AuthManager(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // 1. Fungsi Ambil Token JWT (Sudah ada di kodinganmu)
    fun getToken(): String? {
        return prefs.getString("token", null)
    }

    fun saveToken(token: String) {
        prefs.edit { putString("token", token) }
    }

    // 2. Fungsi Simpan Objek User Utuh dari Laravel
    fun saveUserData(user: UserData) {
        val userJson = gson.toJson(user) // Mengubah objek menjadi String JSON
        prefs.edit { putString("user_data", userJson) }
    }

    // 3. Fungsi Ambil Objek User Utuh (Dipakai di MainActivity & Dashboard)
    fun getUserData(): UserData? {
        val userJson = prefs.getString("user_data", null) ?: return null
        return try {
            gson.fromJson(userJson, UserData::class.java) // Mengembalikan String JSON menjadi Objek Java/Kotlin
        } catch (e: Exception) {
            null
        }
    }

    // 4. 🟢 Fungsi Ambil Nama Role Spesifik (Jika suatu saat kamu butuh String "Super Admin")
    fun getUserRoleName(): String {
        val user = getUserData()
        return user?.role?.role ?: "" // Mengambil properti string 'role' dari nested object
    }

    // 5. Fungsi Hapus Sesi Saat Logout
    fun clearAuth() {
        prefs.edit { clear() }
    }
}