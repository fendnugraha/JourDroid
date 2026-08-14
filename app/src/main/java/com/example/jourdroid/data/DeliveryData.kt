package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class DeliveryData(
    val success: Boolean,
    val message: String,
    @SerializedName("data")
    private val rawData: com.google.gson.JsonElement
) {
    val deliveries: List<DeliveryItem>
        get() {
            val gson = com.google.gson.Gson()
            return try {
                if (rawData.isJsonArray) {
                    gson.fromJson(rawData, object : com.google.gson.reflect.TypeToken<List<DeliveryItem>>() {}.type)
                } else if (rawData.isJsonObject) {
                    val obj = rawData.asJsonObject
                    if (obj.has("deliveries")) {
                        gson.fromJson(obj.get("deliveries"), object : com.google.gson.reflect.TypeToken<List<DeliveryItem>>() {}.type)
                    } else if (obj.has("data")) {
                        gson.fromJson(obj.get("data"), object : com.google.gson.reflect.TypeToken<List<DeliveryItem>>() {}.type)
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

data class DeliveryItem(
    val id: String, // UUID
    @SerializedName("journal_id") val journalId: Int,
    @SerializedName("source_account_id") val sourceAccountId: Int,
    @SerializedName("destination_account_id") val destinationAccountId: Int,
    @SerializedName("courier_id") val courierId: Int? = null,
    @SerializedName("received_by_id") val receivedById: Int? = null,
    @SerializedName("received_at") val receivedAt: String? = null,
    val priority: String? = "low",
    val status: String, // pending, in_transit, delivered, cancelled
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val invoice: String? = null,
    val amount: Long = 0,
    val latitude: String? = null,
    val longitude: String? = null,
    val notes: String? = null,
    val journal: DeliveryJournal? = null,
    @SerializedName("source_account") val sourceAccount: DeliveryAccount? = null,
    @SerializedName("destination_account") val destinationAccount: DeliveryAccount? = null,
    val courier: DeliveryUserRelation? = null,
    val receiver: DeliveryUserRelation? = null
)

data class DeliveryJournal(
    val id: Int,
    val invoice: String? = null,
    val description: String? = null,
    val amount: Long = 0
)

data class DeliveryAccount(
    val id: Int,
    val name: String? = null,
    @SerializedName("warehouse_id") val warehouseId: Int? = null,
    val warehouse: DeliveryWarehouse? = null
)

data class DeliveryWarehouse(
    val id: Int,
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null
)

data class DeliveryUserRelation(
    val id: Int,
    @SerializedName("contact_id") val contactId: Int? = null,
    val contact: DeliveryContact? = null
)

data class DeliveryContact(
    val id: Int,
    val name: String? = null,
    val phone: String? = null
)
