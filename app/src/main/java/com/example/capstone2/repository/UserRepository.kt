package com.example.capstone2.repository

import android.content.Context
import android.util.Log
import com.example.capstone2.MyApp
import com.example.capstone2.R
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.data.models.UpdateProfileRequest
import com.example.capstone2.data.models.ChangePasswordRequest
import com.example.capstone2.network.ApiClient
import retrofit2.Response
import com.example.capstone2.network.getTokenProvider
import com.example.capstone2.repository.SharedPrefManager


class UserRepository(private val context: Context) {

    private val TAG = "LoginRepo"

    private val apiService by lazy {
        ApiClient.getApiService(getTokenProvider(context))
    }

    suspend fun registerUser(registerRequest: RegisterRequest): Response<RegisterResponse> {
        return apiService.register(registerRequest)
    }

    suspend fun loginUser(loginRequest: LoginRequest): Response<LoginResponse> {
        // Diagnostic: log the configured base URL
        try {
            val configuredBase = MyApp.instance.getString(R.string.api_base_url)
            Log.d(TAG, "Configured API base URL: $configuredBase")
        } catch (e: Exception) {
            Log.w(TAG, "Could not read configured API base URL: ${e.message}")
        }

        // Perform the network call with logging around it
        try {
            Log.d(TAG, "Starting login request for email=${loginRequest.emailAddress}")
            val response = apiService.login(loginRequest)

            // Log raw request URL if available
            try {
                val rawReqUrl = response.raw().request.url
                Log.d(TAG, "Login request URL: $rawReqUrl")
            } catch (_: Exception) { }

            if (response.isSuccessful) {
                Log.d(TAG, "Login successful: HTTP ${response.code()}")
                response.body()?.let { body ->
                    body.token?.let { token ->
                        Log.d(TAG, "Received token (length=${token.length})")
                        SharedPrefManager.saveAuthToken(context, token)
                    }
                    body.user?.let { user ->
                        Log.d(TAG, "Received user id=${user.userID} role=${user.roleID}")
                        try {
                            SharedPrefManager.saveUserId(context, user.userID)
                            SharedPrefManager.saveUserStatus(context, user.status)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to save user info: ${e.message}")
                        }
                    }
                } ?: Log.w(TAG, "Login response body was null despite 2xx")
            } else {
                Log.e(TAG, "Login failed: HTTP ${response.code()}")
                try {
                    val err = response.errorBody()?.string()
                    Log.e(TAG, "Login error body: $err")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read error body: ${e.message}")
                }
            }

            return response
        } catch (e: Exception) {
            Log.e(TAG, "Exception during login request: ${e.message}")
            throw e
        }
    }

    suspend fun getProfile(): Response<User> {
        return apiService.getProfile()
    }

    // Update the current user's profile
    suspend fun updateProfile(request: UpdateProfileRequest): Response<User> {
        return apiService.updateProfile(request)
    }

    // Change the current user's password
    suspend fun changePassword(request: ChangePasswordRequest): Response<Map<String, String>> {
        return apiService.changePassword(request)
    }

    suspend fun logout(): Response<Map<String, String>> {
        return apiService.logout()
    }
}
