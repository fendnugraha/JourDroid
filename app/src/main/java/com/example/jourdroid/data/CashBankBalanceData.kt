package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class CashBankBalanceData(
    val success: Boolean,
    val message: String,
    val data: CashBankBalanceItem
)

data class CashBankBalanceItem(
    val chartOfAccounts: List<ChartOfAccounts>,
    val sumtotalBank: Long,
    val sumtotalCash: Long
)

data class ChartOfAccounts(
    val id: Int,
    @SerializedName("account_id") val accountId: Int,
    @SerializedName("acc_name") val accName: String,
    @SerializedName("acc_code") val accCode: String,
    @SerializedName("account_group") val accountGroup: String,
    val balance: Long,
    @SerializedName("warehouse_id") val warehouseId: Int
)
