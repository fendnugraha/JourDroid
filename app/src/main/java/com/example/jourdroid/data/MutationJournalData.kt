package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class MutationJournalData (
    val success: Boolean,
    val message: String,
    val data: List<JournalData>
)

data class JournalData (
    val id: Int = 0,
    @SerializedName("date_issued") val dateIssued: String? = "",
    val invoice: String? = "",
    val description: String? = "",
    val status: Int = 0,
    @SerializedName("trx_type") val trxType: String? = "",
    val amount: Int = 0,
    @SerializedName("fee_amount") val feeAmount: Int = 0,
    val debt: DebtDetail? = null,
    val cred: CredDetail? = null
)

data class DebtDetail (
    val id: Int,
    @SerializedName("acc_name") val accName: String,
    @SerializedName("acc_code") val accCode: String,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("account_group") val accountGroup: String,
    val warehouse: WarehouseDebtDetail?
)

data class WarehouseDebtDetail (
    val id: Int,
    val name: String,
    val code: String
)

data class CredDetail(
    val id: Int,
    @SerializedName("acc_name") val accName: String,
    @SerializedName("acc_code") val accCode: String,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("warehouse_id") val warehouseId: Int,
    @SerializedName("account_group") val accountGroup: String,
    val warehouse: WarehouseCredDetail?
)

data class WarehouseCredDetail (
    val id: Int,
    val name: String,
    val code: String
)
