package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class MutationJournalData (
    val success: Boolean,
    val message: String,
    val data: List<JournalData>
)

data class JournalData (
    val id: Int,
    @SerializedName("date_issued") val dateIssued: String,
    val amount: Int
)