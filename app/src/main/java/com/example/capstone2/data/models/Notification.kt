package com.example.capstone2.data.models

data class Notification(
    val id: Long? = null,
    val userId: Long,
    val title: String,
    val message: String,
    val isRead: Boolean = false,
    val createdAt: String? = null
)

data class NotificationResponse(
    val notifications: List<Notification>
) 