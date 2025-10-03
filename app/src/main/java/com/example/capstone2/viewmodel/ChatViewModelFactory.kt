package com.example.capstone2.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.network.ApiClient
import com.example.capstone2.network.getTokenProvider

class ChatViewModelFactory(private val context: Context, private val currentUserId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            val apiService = ApiClient.getApiService(getTokenProvider(context))
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(apiService, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

