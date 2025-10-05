package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

data class Conversation(
    @SerializedName("conversationID") val conversationID: String,
    @SerializedName("partnerID") val partnerID: Long,
    @SerializedName("lastMessage") val lastMessage: String? = null,
    @SerializedName("lastMessageAt") val lastMessageAt: String? = null,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("partnerName") val partnerName: String? = null
)

