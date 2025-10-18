package com.example.capstone2.service

import android.content.Context
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.NotificationRepository
import com.example.capstone2.util.NotificationUtils
import com.example.capstone2.repository.SharedPrefManager

/**
 * Service for handling app notifications
 */
class NotificationService(
    private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    // Minor use to avoid 'never used' warnings while keeping the repository available for future network saves
    init {
        @Suppress("UNUSED_VARIABLE")
        val _repoRef = notificationRepository
    }

    /**
     * Send a notification when a customer creates a new request
     */
    fun notifyOwnerNewRequest(request: CreateRequest) {
        // Show local notification to owner
        val title = "New Request"
        val message = "New service request received for ${'$'}{request.sackQuantity} sacks"

        // Only show local notification if the developer flag is enabled. Default: false
        if (shouldShowLocalNotifications()) {
            NotificationUtils.showNotification(
                context,
                generateNotificationId(),
                title,
                message
            )
        }

        // Also save to server (simulated since we don't have an endpoint)
        // This would typically be handled by a server push notification
    }

    /**
     * Send a notification when an owner accepts a request
     */
    fun notifyCustomerRequestAccepted(request: Request) {
        val title = "Request Accepted"
        val message = "Your request #${'$'}{request.requestID} has been accepted"

        if (shouldShowLocalNotifications()) {
            NotificationUtils.showNotification(
                context,
                generateNotificationId(),
                title,
                message
            )
        }

        // Also save to server (simulated since we don't have an endpoint)
    }

    /**
     * Send a notification when an owner updates a request status
     */
    fun notifyCustomerStatusUpdate(request: Request, newStatusID: Int) {
        val statusText = getStatusText(newStatusID)

        val title = "Request Status Updated"
        val message = "Your request #${'$'}{request.requestID} status changed to: $statusText"

        if (shouldShowLocalNotifications()) {
            NotificationUtils.showNotification(
                context,
                generateNotificationId(),
                title,
                message
            )
        }

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
            12 -> "Milling done"
            else -> "Unknown status"
        }
    }

    /**
     * Check a shared preference flag to determine whether to show local notifications.
     * This prevents duplicate notifications when server push notifications (Pusher Beams) are used.
     * Default is false.
     */
    private fun shouldShowLocalNotifications(): Boolean {
        return SharedPrefManager.isForceLocalNotificationsEnabled(context)
    }
}
