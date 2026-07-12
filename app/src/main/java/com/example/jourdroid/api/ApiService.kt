package com.example.jourdroid.api

import com.example.jourdroid.data.AccountData
import com.example.jourdroid.data.CashBankBalanceData
import com.example.jourdroid.data.LoginResponse
import com.example.jourdroid.data.MutationJournalData
import com.example.jourdroid.data.WarehouseData
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    // 1. Endpoint untuk Login (Tidak butuh token)
    @FormUrlEncoded
    @POST("api/android/login")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): LoginResponse

    // 2. Endpoint yang butuh Bearer Token (Proteksi Sanctum)
    @GET("api/android/user-profile")
    suspend fun getUserProfile(): LoginResponse

    // logout
    @POST("api/logout")
    suspend fun logout(): retrofit2.Response<Unit>

        // Tambahkan ini untuk test koneksi
    @GET("api/android/test-connection")
    suspend fun testConnection(): retrofit2.Response<Unit> // Cukup return status HTTP (e.g. 200 OK)

    @GET("api/get-all-accounts")
    suspend fun getAllAccounts(): AccountData

    @GET("api/get-all-warehouses")
    suspend fun getAllWarehouses(): WarehouseData


    @GET("api/get-journal-by-warehouse/{warehouse}/{startDate}/{endDate}")
    suspend fun getJournalByWarehouse(
        @Path("warehouse") warehouse: Int,
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String
    ): MutationJournalData

    @GET("api/get-cash-bank-balance/{warehouse}/{endDate}")
    suspend fun getCashBankBalance(
        @Path("warehouse") warehouse: Int,
        @Path("endDate") endDate: String
    ): CashBankBalanceData

    //Transaction
    @FormUrlEncoded
    @POST("api/create-mutation")
    suspend fun createMutation(
        @Field("date_issued") dateIssued: String,
        @Field("debt_code") debtCode: Int,
        @Field("cred_code") credCode: Int,
        @Field("is_confirmed") isConfirmed: Int, 
        @Field("amount") amount: Int,
        @Field("fee_amount") feeAmount: Int,
        @Field("trx_type") trxType: String,
        @Field("description") description: String,
    ): retrofit2.Response<com.example.jourdroid.data.MutationCreateResponse>

}