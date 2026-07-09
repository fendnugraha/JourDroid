package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class LoginResponse (
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserData?
)

data class UserData (
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole?, // Relasi 'role.warehouse' aman di sini

    // 🔴 TAMBAHKAN INI AGAR TIDAK CRASH SAAT MENERIMA ARRAY ATTENDANCES DARI LARAVEL
    val attendances: List<Any>? = emptyList()
)

data class UserRole(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int, // 🟢 Ini akan terbaca dengan sukses!
    val role: String,
    val warehouse: Warehouse?,

)

data class Warehouse(
    val id: Int,
    val name: String?
)