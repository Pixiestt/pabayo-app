package com.example.capstone2.repository

import android.util.Log
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.NotificationResponse
import okhttp3.ResponseBody
import retrofit2.Response

class NotificationRepository(private val apiService: ApiService) {

    suspend fun getNotifications(): Response<NotificationResponse> {
        return apiService.getNotifications()
    }

    suspend fun markNotificationAsRead(notificationId: Long): Response<ResponseBody> {
        return apiService.markNotificationAsRead(notificationId)
    }

    suspend fun deleteNotification(notificationId: Long): Response<ResponseBody> {
        return apiService.deleteNotification(notificationId)
    }
} 