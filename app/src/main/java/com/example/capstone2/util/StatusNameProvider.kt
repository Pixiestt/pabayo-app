package com.example.capstone2.util

/**
 * Centralized provider for statusId -> human-readable name.
 * Keep this in sync with server-side friendlyStatusLabel().
 */
object StatusNameProvider {
    fun getNameFor(statusId: Int): String = when (statusId) {
        1 -> "Subject for approval"
        2 -> "Delivery boy pickup"
        3 -> "Waiting for customer drop off"
        4 -> "Pending"
        5 -> "Processing"
        6 -> "Rider out for delivery"
        7 -> "Waiting for customer to claim"
        8 -> "Completed"
        9 -> "Rejected"
        10 -> "Request Accepted"
        11 -> "Partially Accepted"
        12 -> "Milling done"
        13 -> "Delivered"
        else -> "Unknown status #$statusId"
    }
}

