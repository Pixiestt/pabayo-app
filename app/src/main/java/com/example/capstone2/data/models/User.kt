package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userID") val userID: Long,
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
    val contactNumber: String,
    val homeAddress: String,
    val IDCard: String,
    val roleID: Long,
    val password: String


)
