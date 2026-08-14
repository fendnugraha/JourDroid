package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class ProductData(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    private val rawData: com.google.gson.JsonElement
) {
    val products: List<ProductItem>
        get() {
            val gson = com.google.gson.Gson()
            return try {
                if (rawData.isJsonArray) {
                    gson.fromJson(rawData, object : com.google.gson.reflect.TypeToken<List<ProductItem>>() {}.type)
                } else if (rawData.isJsonObject) {
                    val obj = rawData.asJsonObject
                    if (obj.has("products")) {
                        gson.fromJson(obj.get("products"), object : com.google.gson.reflect.TypeToken<List<ProductItem>>() {}.type)
                    } else if (obj.has("data")) {
                        gson.fromJson(obj.get("data"), object : com.google.gson.reflect.TypeToken<List<ProductItem>>() {}.type)
                    } else {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}

data class ProductItem(
    val id: Int,
    val name: String,
    val code: String? = null,
    val price: String? = "0", // Changed to String because API returns "15000.00"
    val cost: String? = "0",
    @SerializedName("end_stock") val stock: Int = 0, // Mapped from end_stock
    val unit: String? = null,
    val category: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null
) {
    // Helper to get price as Long for calculations
    val priceLong: Long
        get() = try {
            price?.toDouble()?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
}
