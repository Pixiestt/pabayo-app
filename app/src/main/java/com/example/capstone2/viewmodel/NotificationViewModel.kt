package com.example.capstone2.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.Notification
import com.example.capstone2.repository.NotificationRepository
import com.example.capstone2.util.NotificationUtils
import kotlinx.coroutines.launch

class NotificationViewModel(
    application: Application,
    private val notificationRepository: NotificationRepository
) : AndroidViewModel(application) {

    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        // Initialize notification channel
        NotificationUtils.createNotificationChannel(application)
    }
    
    fun fetchNotifications() {
        _loading.value = true
        viewModelScope.launch {
            try {
                val response = notificationRepository.getNotifications()
                if (response.isSuccessful) {
                    response.body()?.notifications?.let {
                        _notifications.value = it
                    }
                    _error.value = null
                } else {
                    _error.value = "Failed to fetch notifications: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Error fetching notifications: ${e.message}"
                Log.e("NotificationViewModel", "Error fetching notifications", e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                val response = notificationRepository.markNotificationAsRead(notificationId)
                if (response.isSuccessful) {
                    // Update local notifications list
                    _notifications.value = _notifications.value?.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                } else {
                    _error.value = "Failed to mark notification as read"
                }
            } catch (e: Exception) {
                _error.value = "Error marking notification as read: ${e.message}"
                Log.e("NotificationViewModel", "Error marking notification as read", e)
            }
        }
    }
    
    fun deleteNotification(notificationId: Long) {
        viewModelScope.launch {
            try {
                val response = notificationRepository.deleteNotification(notificationId)
                if (response.isSuccessful) {
                    // Remove notification from local list
                    _notifications.value = _notifications.value?.filter { it.id != notificationId }
                } else {
                    _error.value = "Failed to delete notification"
                }
            } catch (e: Exception) {
                _error.value = "Error deleting notification: ${e.message}"
                Log.e("NotificationViewModel", "Error deleting notification", e)
            }
        }
    }
    
    // Show a local notification
    fun showLocalNotification(title: String, message: String, notificationId: Int = (0..1000).random()) {
        val context: Context = getApplication()
        NotificationUtils.showNotification(context, notificationId, title, message)
    }
} 