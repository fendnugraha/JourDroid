package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class TransactionRequest(
    val cart: List<CartItem>,
    @SerializedName("transaction_type") val transactionType: String = "Sales",
    @SerializedName("warehouse_id") val warehouseId: Int? = null,
    val description: String? = ""
)

data class CartItem(
    @SerializedName("id") val productId: Int,
    val quantity: Int,
    val price: Long
)

data class SalesTransactionResponse(
    val success: Boolean,
    val message: String,
    val data: SalesData? = null,
    val invoice: String? = null,
    @SerializedName("transactions") val transactions: List<SalesData>? = null
)

data class SalesData(
    val id: Int,
    val invoice: String?,
    @SerializedName("date_issued") val dateIssued: String?,
    val amount: Long,
    val status: Int,
    val items: List<SalesItem>? = null
)

data class SalesItem(
    val id: Int,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    val price: Long,
    val subtotal: Long
)
