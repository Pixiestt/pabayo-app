package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

/**
 * Minimal message model used by the app and API.
 * Fields kept simple so JSON parsing works with common backends.
 */
data class Message(
    @SerializedName("messageID") val id: Long? = null,
    @SerializedName("senderID") val senderID: Long = -1L,
    @SerializedName("receiverID") val receiverID: Long = -1L,
    @SerializedName("message") val message: String = "",
    @SerializedName("createdAt") val timestamp: String? = null,
    @SerializedName("conversationID") val conversationID: String? = null,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("receiverName") val receiverName: String? = null
)
