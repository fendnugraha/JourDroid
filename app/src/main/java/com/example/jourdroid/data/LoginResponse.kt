package com.example.jourdroid.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class LoginResponse (
    val success: Boolean,
    val message: String? = null,
    val token: String? = null,
    @SerializedName("token_type") val tokenType: String? = null,
    val user: UserData? = null
)

data class UserData (
    val id: Int,
    val name: String? = null,
    val email: String? = null,
    @SerializedName("email_verified_at") val emailVerifiedAt: String? = null,
    @SerializedName("contact_id") val contactId: Int? = null,
    @SerializedName("warehouse_id") val warehouseId: Int? = null,
    val role: Any? = null, 
    val latitude: String? = null,
    val longitude: String? = null,
    @SerializedName("fcm_token") val fcmToken: String? = null,
    @SerializedName("is_active") val isActive: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("has_checked_in") val hasCheckedIn: Boolean? = null,
    val warehouse: Warehouse? = null,
    val contact: ContactData? = null,
    val attendances: List<Any>? = emptyList()
)

data class Warehouse(
    val id: Int,
    val code: String? = null,
    val name: String? = null,
    val status: Int? = null,
    @SerializedName("opening_time") val openingTime: String? = null,
    @SerializedName("is_open") val isOpen: Int? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val address: String? = null,
    @SerializedName("warehouse_zone_id") val warehouseZoneId: Int? = null,
    @SerializedName("ownership_status") val ownershipStatus: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("primary_cash") val primaryCash: PrimaryCash? = null
)

data class PrimaryCash(
    val id: Int,
    val code: String? = null,
    val name: String? = null,
    val group: String? = null,
    @SerializedName("account_id") val accountId: Int? = null,
    @SerializedName("warehouse_id") val warehouseId: Int? = null,
    @SerializedName("is_primary_cash") val isPrimaryCash: Int? = null,
    @SerializedName("st_balance") val stBalance: Long? = null,
    @SerializedName("is_locked") val isLocked: Int? = null,
    @SerializedName("limit") private val rawLimit: JsonElement? = null
) {
    val limit: Long?
        get() = try {
            when {
                rawLimit == null || rawLimit.isJsonNull -> null
                rawLimit.isJsonPrimitive -> rawLimit.asLong
                rawLimit.isJsonObject -> {
                    val obj = rawLimit.asJsonObject
                    when {
                        obj.has("limit") -> obj.get("limit").asLong
                        obj.has("amount") -> obj.get("amount").asLong
                        obj.has("value") -> obj.get("value").asLong
                        else -> null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
}

data class ContactData(
    val id: Int,
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("telegram_chat_id") val telegramChatId: String? = null,
    @SerializedName("contact_photo_url") val contactPhotoUrl: String? = null,
    val address: String? = null,
    val photo: String? = null,
    @SerializedName("user_id") val userId: Int? = null,
    val employee: EmployeeData? = null,
    @SerializedName("employee_receivables_sum") val employeeReceivablesSum: ReceivableSum? = null,
    @SerializedName("installment_receivables_sum") val installmentReceivablesSum: ReceivableSum? = null
)

data class ReceivableSum(
    val total: Double? = null
)

data class EmployeeData(
    val id: Int,
    @SerializedName("contact_id") val contactId: Int? = null,
    @SerializedName("hire_date") val hireDate: String? = null,
    @SerializedName("id_card_number") val idCardNumber: String? = null,
    val gender: String? = null,
    @SerializedName("birth_date") val birthDate: String? = null,
    @SerializedName("place_of_birth") val placeOfBirth: String? = null,
    val religion: String? = null,
    @SerializedName("marital_status") val maritalStatus: String? = null,
    @SerializedName("employment_type") val employmentType: String? = null,
    @SerializedName("base_salary") val baseSalary: Long? = null,
    @SerializedName("contract_start") val contractStart: String? = null,
    @SerializedName("contract_end") val contractEnd: String? = null,
    val note: String? = null,
    val status: String? = null,
    @SerializedName("warning_active") val warningActive: WarningData? = null
)

data class WarningData(
    val level: String? = null,
    @SerializedName("letter_number") val letterNumber: String? = null,
    @SerializedName("expired_date") val expiredDate: String? = null,
    val reason: String? = null
)

data class ContactUpdateResponse(
    val status: String,
    val message: String,
    val data: ContactData
)
