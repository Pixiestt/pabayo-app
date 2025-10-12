package com.example.capstone2.data.models

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("emailAddress") val emailAddress: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("password") val password: String
)
