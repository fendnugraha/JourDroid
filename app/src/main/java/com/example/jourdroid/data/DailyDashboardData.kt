package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

// This is now the Top Level object returned by the API
data class DailyDashboardResponse(
    val success: Boolean?,
    val message: String?,
    val data: DailyDashboardData?
)

data class DailyDashboardData(
    val totalCash: Int,
    val totalBank: Int,
    val totalTransfer: SummaryData?,
    val totalCashWithdrawal: SummaryData?,
    val totalCashDeposit: SummaryData?,
    val totalVoucher: SummaryData?,
    val totalAccessories: SummaryData?,
    val totalExpense: Int,
    val totalBankFee: Int,
    val totalFee: Int,
    val totalCorrection: Int,
    val profit: Int,
    val countDays: Int,
    val diffDays: Int,
    val averageProfit: Int,
    val salesCount: Int
)

data class SummaryData(
    val total: Int,
    val count: Int
)
