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
    val invoice: String,
    val description: String,
    @SerializedName("trx_type") val trxType: String,
    val amount: Int
)