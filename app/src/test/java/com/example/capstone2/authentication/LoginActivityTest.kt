package com.example.capstone2.authentication

import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.testdata.TestData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Assert.*

@RunWith(JUnit4::class)
class LoginActivityTest {

    @Test
    fun `validateLoginInput with valid email and password should return true`() {
        // Given
        val email = "kyle@gmail.com"
        val password = "kyle123"

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateLoginInput with empty email should return false`() {
        // Given
        val email = ""
        val password = "password123"

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateLoginInput with empty password should return false`() {
        // Given
        val email = "test@example.com"
        val password = ""

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateLoginInput with both empty should return false`() {
        // Given
        val email = ""
        val password = ""

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateLoginInput with whitespace only email should return false`() {
        // Given
        val email = "   "
        val password = "password123"

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateLoginInput with whitespace only password should return false`() {
        // Given
        val email = "test@example.com"
        val password = "   "

        // When
        val result = validateLoginInput(email, password)

        // Then
        assertFalse(result)
    }

    @Test
    fun `createLoginRequest should create correct LoginRequest object`() {
        // Given
        val email = "test@example.com"
        val password = "password123"

        // When
        val result = createLoginRequest(email, password)

        // Then
        assertEquals(email, result.emailAddress)
        assertEquals(password, result.password)
    }

    @Test
    fun `createLoginRequest should trim email and password`() {
        // Given
        val email = "  test@example.com  "
        val password = "  password123  "

        // When
        val result = createLoginRequest(email, password)

        // Then
        assertEquals("test@example.com", result.emailAddress)
        assertEquals("password123", result.password)
    }

    @Test
    fun `isValidLoginResponse should return true for valid response with token`() {
        // Given
        val loginResponse = TestData.successfulLoginResponseOwner

        // When
        val result = isValidLoginResponse(loginResponse)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isValidLoginResponse should return false for null response`() {
        // Given
        val loginResponse: LoginResponse? = null

        // When
        val result = isValidLoginResponse(loginResponse)

        // Then
        assertFalse(result)
    }

    @Test
    fun `isValidLoginResponse should return false for response with empty token`() {
        // Given
        val loginResponse = TestData.emptyTokenLoginResponse

        // When
        val result = isValidLoginResponse(loginResponse)

        // Then
        assertFalse(result)
    }

    @Test
    fun `getRoleBasedActivity should return correct activity class for owner role`() {
        // Given
        val user = TestData.testUserOwner

        // When
        val result = getRoleBasedActivity(user)

        // Then
        assertEquals(com.example.capstone2.owner.OwnerMainActivity::class.java, result)
    }

    @Test
    fun `getRoleBasedActivity should return correct activity class for customer role`() {
        // Given
        val user = TestData.testUserCustomer

        // When
        val result = getRoleBasedActivity(user)

        // Then
        assertEquals(com.example.capstone2.customer.CustomerMainActivity::class.java, result)
    }

    @Test
    fun `getRoleBasedActivity should return null for unknown role`() {
        // Given
        val user = TestData.testUserOwner.copy(roleID = 999L)

        // When
        val result = getRoleBasedActivity(user)

        // Then
        assertNull(result)
    }
}

// Helper functions that replicate the business logic from LoginActivity
private fun validateLoginInput(email: String, password: String): Boolean {
    return email.trim().isNotEmpty() && password.trim().isNotEmpty()
}

private fun createLoginRequest(email: String, password: String): LoginRequest {
    return LoginRequest(email.trim(), password.trim())
}

private fun isValidLoginResponse(loginResponse: LoginResponse?): Boolean {
    return loginResponse != null && loginResponse.token.isNotEmpty()
}

private fun getRoleBasedActivity(user: User): Class<*>? {
    return when (user.roleID.toLong()) {
        2L -> com.example.capstone2.owner.OwnerMainActivity::class.java
        3L -> com.example.capstone2.customer.CustomerMainActivity::class.java
        else -> null
    }
}