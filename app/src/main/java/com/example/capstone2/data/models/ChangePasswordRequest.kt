package com.example.capstone2.data.models

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

