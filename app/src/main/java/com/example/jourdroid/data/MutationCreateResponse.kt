package com.example.jourdroid.data

data class MutationCreateResponse(
    val success: Boolean,
    val message: String,
    val data: JournalData?
)
