package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

/**
 * Flexible data model to handle different API response formats for accounts.
 * Supports:
 * 1. { "data": [ ... ] }
 * 2. { "data": { "accounts": [ ... ] } }
 * 3. { "data": { "chartOfAccounts": [ ... ] } }
 */
data class AccountData(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    private val rawData: com.google.gson.JsonElement
) {
    val accounts: List<AccountItem>
        get() {
            val gson = com.google.gson.Gson()
            return try {
                if (rawData.isJsonArray) {
                    // Case 1: data is [ ... ]
                    gson.fromJson(rawData, object : com.google.gson.reflect.TypeToken<List<AccountItem>>() {}.type)
                } else if (rawData.isJsonObject) {
                    val obj = rawData.asJsonObject
                    if (obj.has("accounts")) {
                        // Case 2: data is { "accounts": [ ... ] }
                        gson.fromJson(obj.get("accounts"), object : com.google.gson.reflect.TypeToken<List<AccountItem>>() {}.type)
                    } else if (obj.has("chartOfAccounts")) {
                        // Case 3: data is { "chartOfAccounts": [ ... ] }
                        gson.fromJson(obj.get("chartOfAccounts"), object : com.google.gson.reflect.TypeToken<List<AccountItem>>() {}.type)
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

data class AccountItem(
    val id: Int,
    @SerializedName("name") val accName: String,
    @SerializedName("code") val accCode: String,
    @SerializedName("group") val accountGroup: String,
    val account: ParentAccountData? = null,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("is_locked") val isLocked: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("st_balance") val stBalance: Double
)

data class ParentAccountData(
    val id: Int,
    val name: String? = null,
    val code: String? = null,
    val type: String? = null,
    val status: String? = null
)
