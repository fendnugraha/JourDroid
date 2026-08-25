package com.example.jourdroid.data

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    val success: Boolean,
    @SerializedName("unread_count") val unreadCount: Int,
    val data: NotificationPagination
)

data class NotificationPagination(
    @SerializedName("current_page") val currentPage: Int,
    val data: List<NotificationItem>,
    @SerializedName("last_page") val lastPage: Int,
    val total: Int
)

data class NotificationItem(
    val id: String,
    val type: String,
    val data: Map<String, Any?>,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String?
) {
    // Helpers to extract common fields from data map
    val title: String get() = data["title"]?.toString() ?: "Notification"
    val message: String get() = data["message"]?.toString() ?: data["body"]?.toString() ?: ""
    val isRead: Boolean get() = readAt != null
}
