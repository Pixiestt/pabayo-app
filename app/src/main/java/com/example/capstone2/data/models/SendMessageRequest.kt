// new file
package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

/** Minimal request body for sending a chat message */
data class SendMessageRequest(
    @SerializedName("receiverID") val receiverID: Long,
    @SerializedName("message") val message: String,
    @SerializedName("conversationID") val conversationID: String? = null
)
