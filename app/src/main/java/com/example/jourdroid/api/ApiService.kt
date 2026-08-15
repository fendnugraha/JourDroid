package com.example.jourdroid.api

import com.example.jourdroid.data.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ApiService {
    // 1. Endpoint untuk Login (Tidak butuh token)
    @POST("api/android/login")
    suspend fun login(
        @Body request: LoginRequest
    ): retrofit2.Response<LoginResponse>

    // 2. Endpoint yang butuh Bearer Token (Proteksi Sanctum)
    @GET("api/android/user-profile")
    suspend fun getUserProfile(): LoginResponse

    @GET("api/checkin-status")
    suspend fun getUserCheckedInStatus(): CheckInStatusResponse

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

    @GET("api/get-all-products")
    suspend fun getAllProducts(): ProductData

    @GET("api/get-nearest-warehouse")
    suspend fun getNearestWarehouse(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): NearestWarehouseResponse

    //Deliveries
    @GET("api/deliveries")
    suspend fun getDeliveries(): DeliveryData

    @FormUrlEncoded
    @POST("api/deliveries/{id}/process")
    suspend fun processDelivery(
        @Path("id") id: String,
        @Field("latitude") latitude: Double?,
        @Field("longitude") longitude: Double?
    ): retrofit2.Response<Unit>

    @FormUrlEncoded
    @POST("api/deliveries/{id}/complete")
    suspend fun completeDelivery(
        @Path("id") id: String,
        @Field("latitude") latitude: Double?,
        @Field("longitude") longitude: Double?,
        @Field("amount") amount: Long?,
        @Field("note") note: String?
    ): retrofit2.Response<Unit>

    @POST("api/deliveries/{id}/cancel")
    suspend fun cancelDelivery(@Path("id") id: String): retrofit2.Response<Unit>


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

    // POS / Transactions
    @POST("api/transactions")
    suspend fun submitTransaction(
        @Body request: TransactionRequest
    ): SalesTransactionResponse

    @GET("api/get-tx-by-warehouse/{warehouse}/{startDate}/{endDate}")
    suspend fun getTxByWarehouse(
        @Path("warehouse") warehouse: Int,
        @Path("startDate") startDate: String,
        @Path("endDate") endDate: String
    ): SalesTransactionResponse

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

    @PUT("api/update-delivery-status/{id}/{status}")
    suspend fun updateJournalStatus(
        @Path("id") id: Int,
        @Path("status") status: Int
    ): retrofit2.Response<Unit>

    @FormUrlEncoded
    @POST("api/update-fcm-token")
    suspend fun updateFcmToken(
        @Field("fcm_token") token: String
    ): retrofit2.Response<Unit>

    @Multipart
    @POST("api/create-attendance")
    suspend fun createAttendance(
        @Part photo: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("warehouse_id") warehouseId: RequestBody,
        @Part("role") role: RequestBody,
        @Part("time_in") timeIn: RequestBody
    ): retrofit2.Response<Unit>

}

data class LoginRequest(
    val email: String,
    val password: String
)

