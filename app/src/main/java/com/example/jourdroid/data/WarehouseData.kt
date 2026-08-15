package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

/**
 * Flexible data model to handle different API response formats for warehouses.
 * Supports:
 * 1. { "data": [ ... ] }
 * 2. { "data": { "warehouses": [ ... ] } }
 */
data class WarehouseData(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    private val rawData: com.google.gson.JsonElement
) {
    val warehouses: List<WarehouseItem>
        get() {
            val gson = com.google.gson.Gson()
            return try {
                if (rawData.isJsonArray) {
                    // Case 1: data is [ ... ]
                    gson.fromJson(rawData, object : com.google.gson.reflect.TypeToken<List<WarehouseItem>>() {}.type)
                } else if (rawData.isJsonObject) {
                    val obj = rawData.asJsonObject
                    if (obj.has("warehouses")) {
                        // Case 2: data is { "warehouses": [ ... ] }
                        gson.fromJson(obj.get("warehouses"), object : com.google.gson.reflect.TypeToken<List<WarehouseItem>>() {}.type)
                    } else if (obj.has("data")) {
                         // Alternate check for { "data": { "data": [ ... ] } }
                         gson.fromJson(obj.get("data"), object : com.google.gson.reflect.TypeToken<List<WarehouseItem>>() {}.type)
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

data class WarehouseItem(
    val id: Int,
    val name: String,
    val code: String,
    val address: String? = null,
    @SerializedName("chart_of_account_id") val chartOfAccountId: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("opening_time") val openingTime: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distance: Double? = null,
    val status: Int = 1,
    @SerializedName("contact_id") val contactId: Int? = null,
    @SerializedName("warehouse_zone_id") val warehouseZoneId: Int? = null
)
