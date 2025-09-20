package com.example.capstone2.testdata

import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.User

object TestData {
    
    // Test Users
    val testUserOwner = User(
        userID = 1L,
        firstName = "John",
        lastName = "Doe",
        emailAddress = "john.doe@example.com",
        contactNumber = "1234567890",
        homeAddress = "123 Main St",
        IDCard = "ID123456",
        roleID = 2L, // Owner role
        password = "password123"
    )
    
    val testUserCustomer = User(
        userID = 2L,
        firstName = "Jane",
        lastName = "Smith",
        emailAddress = "jane.smith@example.com",
        contactNumber = "0987654321",
        homeAddress = "456 Oak Ave",
        IDCard = "ID789012",
        roleID = 3L, // Customer role
        password = "password456"
    )
    
    // Test Login Requests
    val validLoginRequestOwner = LoginRequest(
        emailAddress = "john.doe@example.com",
        password = "password123"
    )
    
    val validLoginRequestCustomer = LoginRequest(
        emailAddress = "jane.smith@example.com",
        password = "password456"
    )
    
    val invalidLoginRequest = LoginRequest(
        emailAddress = "invalid@example.com",
        password = "wrongpassword"
    )
    
    val emptyEmailLoginRequest = LoginRequest(
        emailAddress = "",
        password = "password123"
    )
    
    val emptyPasswordLoginRequest = LoginRequest(
        emailAddress = "john.doe@example.com",
        password = ""
    )
    
    // Test Login Responses
    val successfulLoginResponseOwner = LoginResponse(
        user = testUserOwner,
        token = "valid_token_123"
    )
    
    val successfulLoginResponseCustomer = LoginResponse(
        user = testUserCustomer,
        token = "valid_token_456"
    )
    
    val emptyTokenLoginResponse = LoginResponse(
        user = testUserOwner,
        token = ""
    )
}
