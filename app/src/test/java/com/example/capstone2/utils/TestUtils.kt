package com.example.capstone2.utils

import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.User
import io.mockk.mockk
import retrofit2.Response

object TestUtils {

    /**
     * Creates a successful login response for testing
     */
    fun createSuccessfulLoginResponse(
        user: User,
        token: String = "test_token_${System.currentTimeMillis()}"
    ): Response<LoginResponse> {
        val loginResponse = LoginResponse(user = user, token = token)
        return Response.success(loginResponse)
    }

    /**
     * Creates a failed login response for testing
     */
    fun createFailedLoginResponse(errorCode: Int = 401): Response<LoginResponse> {
        return Response.error(errorCode, mockk())
    }

    /**
     * Creates a login request with trimmed values
     */
    fun createLoginRequest(email: String, password: String): LoginRequest {
        return LoginRequest(
            emailAddress = email.trim(),
            password = password.trim()
        )
    }

    /**
     * Validates that a login response is valid (not null and has non-empty token)
     */
    fun isValidLoginResponse(loginResponse: LoginResponse?): Boolean {
        return loginResponse != null && loginResponse.token.isNotEmpty()
    }

    /**
     * Validates login input (email and password are not empty after trimming)
     */
    fun isValidLoginInput(email: String, password: String): Boolean {
        return email.trim().isNotEmpty() && password.trim().isNotEmpty()
    }

    /**
     * Creates a mock user with specified role
     */
    fun createMockUser(
        userID: Long = 1L,
        firstName: String = "Test",
        lastName: String = "User",
        emailAddress: String = "test@example.com",
        roleID: Long = 2L
    ): User {
        return User(
            userID = userID,
            firstName = firstName,
            lastName = lastName,
            emailAddress = emailAddress,
            contactNumber = "1234567890",
            homeAddress = "123 Test St",
            IDCard = "ID123456",
            roleID = roleID,
            password = "password123"
        )
    }

    /**
     * Waits for LiveData to emit a value (for testing purposes)
     */
    suspend fun waitForLiveDataValue() {
        kotlinx.coroutines.delay(100) // Small delay to allow LiveData to emit
    }
}
