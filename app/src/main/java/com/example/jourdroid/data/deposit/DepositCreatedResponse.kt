package com.example.jourdroid.data.deposit

import com.example.jourdroid.data.JournalData


data class DepositCreatedResponse(
    val success: Boolean,
    val message: String,
    val data: JournalData?
)