package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class CheckInStatusResponse(
    @SerializedName("has_checked_in")
    val hasCheckedIn: Boolean
)
