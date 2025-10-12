package com.example.capstone2.service

import android.content.Context
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.NotificationRepository
import com.example.capstone2.repository.SharedPrefManager

/**
 * Factory class for creating NotificationService instances
 */
object NotificationServiceFactory {
    
    private var instance: NotificationService? = null
    
    /**
     * Get a singleton instance of NotificationService
     */
    fun getInstance(context: Context): NotificationService {
        if (instance == null) {
            // Get token from centralized SharedPrefManager
            val token = SharedPrefManager.getAuthToken(context) ?: ""

            // Create API service
            val apiService = ApiClient.getApiService { token }

            // Create repository
            val notificationRepository = NotificationRepository(apiService)
            
            // Create and store instance
            instance = NotificationService(context.applicationContext, notificationRepository)
        }
        return instance!!
    }
}
