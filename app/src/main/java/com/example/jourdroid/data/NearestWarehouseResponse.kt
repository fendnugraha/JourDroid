package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class NearestWarehouseResponse(
    val success: Boolean? = true,
    val found: Boolean,
    val message: String? = null,
    val warehouse: WarehouseItem? = null
)
