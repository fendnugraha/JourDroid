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
    val success: Boolean = true,
    val message: String? = null,
    val data: SalesData? = null,
    val invoice: String? = null,
    @SerializedName("transactions") val transactions: List<SalesData>? = null
)

data class SalesActivityItem(
    val id: Int,
    @SerializedName("date_issued") val dateIssued: String?,
    val invoice: String?,
    @SerializedName("product_id") val productId: Int,
    val quantity: Int,
    val price: String?,
    val cost: String?,
    @SerializedName("transaction_type") val transactionType: String?,
    @SerializedName("warehouse_id") val warehouseId: Int?,
    @SerializedName("user_id") val userId: Int?,
    val status: Int,
    val product: ProductActivityInfo?,
    val contact: ContactActivityInfo?
)

data class ProductActivityInfo(
    val id: Int,
    val name: String,
    val category: String?,
    val price: String?
)

data class ContactActivityInfo(
    val id: Int,
    val name: String,
    val phone: String?
)

data class SalesSummaryItem(
    @SerializedName("product_id") val productId: Int,
    val quantity: String?,
    @SerializedName("total_price") val totalPrice: String?,
    @SerializedName("total_cost") val totalCost: String?,
    val product: ProductActivityInfo?
)

data class SalesData(
    val id: Int = 0,
    val invoice: String? = null,
    @SerializedName("date_issued") val dateIssued: String? = null,
    val amount: Long = 0,
    val status: Int = 0,
    val items: List<SalesItem>? = null,
    val list: List<SalesActivityItem>? = null,
    val summary: List<SalesSummaryItem>? = null
)

data class SalesItem(
    val id: Int,
    @SerializedName("product_name") val productName: String,
    val quantity: Int,
    val price: Long,
    val subtotal: Long
)
