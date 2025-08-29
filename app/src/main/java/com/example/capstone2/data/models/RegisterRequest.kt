package com.example.capstone2.data.models

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val emailAddress: String,
    val contactNumber: String,
    val homeAddress: String,
    val IDCard: String,
    val roleID: Int,
    val password: String
)
