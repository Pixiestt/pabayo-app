package com.example.capstone2.service

import android.content.Context
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.Notification
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.NotificationRepository
import com.example.capstone2.util.NotificationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service for handling app notifications
 */
class NotificationService(
    private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    /**
     * Send a notification when a customer creates a new request
     */
    fun notifyOwnerNewRequest(request: CreateRequest) {
        // Show local notification to owner
        val title = "New Request"
        val message = "New service request received for ${request.sackQuantity} sacks"
        
        // Show a system notification
        NotificationUtils.showNotification(
            context,
            generateNotificationId(),
            title,
            message
        )
        
        // Also save to server (simulated since we don't have an endpoint)
        // This would typically be handled by a server push notification
    }
    
    /**
     * Send a notification when an owner accepts a request
     */
    fun notifyCustomerRequestAccepted(request: Request) {
        val title = "Request Accepted"
        val message = "Your request #${request.requestID} has been accepted"
        
        // Show a system notification
        NotificationUtils.showNotification(
            context,
            generateNotificationId(),
            title,
            message
        )
        
        // Also save to server (simulated since we don't have an endpoint)
    }
    
    /**
     * Send a notification when an owner updates a request status
     */
    fun notifyCustomerStatusUpdate(request: Request, newStatusID: Int) {
        val statusText = getStatusText(newStatusID)
        
        val title = "Request Status Updated"
        val message = "Your request #${request.requestID} status changed to: $statusText"
        
        // Show a system notification
        NotificationUtils.showNotification(
            context,
            generateNotificationId(),
            title,
            message
        )
        
        // Also save to server (simulated since we don't have an endpoint)
    }
    
    /**
     * Generate a unique notification ID
     */
    private fun generateNotificationId(): Int {
        return (System.currentTimeMillis() % 10000).toInt()
    }
    
    /**
     * Get text description for status ID
     */
    private fun getStatusText(statusId: Int): String {
        return when (statusId) {
            1 -> "Subject for approval"
            2 -> "Delivery boy pickup"
            3 -> "Waiting for customer drop off"
            4 -> "Pending"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer pickup"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
            else -> "Unknown status"
        }
    }
} 