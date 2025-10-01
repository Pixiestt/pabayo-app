package com.example.capstone2.data.models

// Fields that the user is allowed to update from their profile
data class UpdateProfileRequest(
    val firstName: String?,
    val lastName: String?,
    val emailAddress: String?,
    val contactNumber: String?,
    val homeAddress: String?
)
