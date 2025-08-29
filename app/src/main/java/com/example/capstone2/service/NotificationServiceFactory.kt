package com.example.capstone2.service

import android.content.Context
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.NotificationRepository

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
            // Get token from shared preferences
            val sharedPreferences = context.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val token = sharedPreferences.getString("auth_token", "")
            
            // Create API service
            val apiService = ApiClient.getApiService { token ?: "" }
            
            // Create repository
            val notificationRepository = NotificationRepository(apiService)
            
            // Create and store instance
            instance = NotificationService(context.applicationContext, notificationRepository)
        }
        return instance!!
    }
} 