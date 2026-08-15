package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class AttendanceResponse(
    val success: Any?,
    val message: String?,
    val data: AttendanceData?
)

data class AttendanceData(
    val id: Any?,
    @SerializedName("user_id") val userId: Any?,
    @SerializedName("warehouse_id") val warehouseId: Any?,
    val photo: String?,
    @SerializedName("time_in") val timeIn: Any?,
    val date: Any?,
    val note: String?,
    val longitude: String?,
    val latitude: String?,
    @SerializedName("approval_status") val approvalStatus: Any?,
    @SerializedName("warehouse_name") val warehouseName: String? = null
)
