package com.example.jourdroid.data

data class LoginResponse (
    val success: Boolean,
    val message: String,
    val token: String?,
    val user: UserData?
)

data class UserData (
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)