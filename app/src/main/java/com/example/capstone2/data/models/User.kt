package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userID") val userID: Long,
    val firstName: String?,
    val lastName: String?,
    val emailAddress: String?,
    val contactNumber: String?,
    val homeAddress: String?,
    val IDCard: String?,
    val roleID: Long?,
    val password: String?,

    // Optional account status (e.g. "approved" or "pending").
    // This assumes the API returns a field named `status`; if your API uses a different name,
    // change the SerializedName accordingly.
    val status: String? = null
)
