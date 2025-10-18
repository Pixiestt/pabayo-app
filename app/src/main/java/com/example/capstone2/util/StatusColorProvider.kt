package com.example.capstone2.util

import android.graphics.Color

/**
 * Centralized provider for status -> color mapping.
 * Returns an Android color int.
 */
object StatusColorProvider {
    fun getColorFor(statusId: Int): Int {
        val hex = when (statusId) {
            // Explicit mappings
            1 -> "#F44336"      // Subject for approval = Red
            12, 7, 8, 13 -> "#4CAF50" // Milling done, Waiting for customer to claim, Completed, Delivered = Green
            2 -> "#FB8C00"      // Delivery boy pickup = Orange
            3 -> "#FFC107"      // Waiting for customer drop off = Amber
            4 -> "#2196F3"      // Pending = Blue
            5 -> "#9C27B0"      // Processing = Purple
            6 -> "#FF5722"      // Rider out for delivery = Deep Orange
            9 -> "#9E9E9E"      // Rejected = Grey
            10 -> "#009688"     // Request Accepted = Teal
            11 -> "#00BCD4"     // Partially Accepted = Cyan
            else -> "#607D8B"   // Fallback = Blue Grey
        }
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            // Fallback to a default blue-grey if parse fails
            Color.parseColor("#607D8B")
        }
    }
}
