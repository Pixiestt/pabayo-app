package com.example.capstone2.repository

import android.content.Context
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.network.ApiClient
import retrofit2.Response
import com.example.capstone2.network.getTokenProvider


class UserRepository(private val context: Context) {

    private val apiService by lazy {
        ApiClient.getApiService(getTokenProvider(context))
    }

    suspend fun registerUser(registerRequest: RegisterRequest): Response<RegisterResponse> {
        return apiService.register(registerRequest)
    }

    suspend fun loginUser(loginRequest: LoginRequest): Response<LoginResponse> {
        val response = apiService.login(loginRequest)
        if (response.isSuccessful) {
            response.body()?.token?.let { token ->
                SharedPrefManager(context).saveAuthToken(token)
            }
        }
        return response
    }

    suspend fun getProfile(): Response<User> {
        return apiService.getProfile()
    }

    suspend fun logout(): Response<Map<String, String>> {
        return apiService.logout()
    }
}
