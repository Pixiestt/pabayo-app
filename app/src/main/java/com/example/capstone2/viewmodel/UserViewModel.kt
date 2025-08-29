package com.example.capstone2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.repository.UserRepository
import retrofit2.Response

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {


    // Register a new user
    fun registerUser(registerRequest: RegisterRequest) = liveData {
        val response: Response<RegisterResponse> = userRepository.registerUser(registerRequest)
        if (response.isSuccessful) {
            emit(response.body())  // Emit successful response
        } else {
            emit(null)  // Emit null if failed
        }
    }

    // Login a user
    fun loginUser(loginRequest: LoginRequest) = liveData {
        val response: Response<LoginResponse> = userRepository.loginUser(loginRequest)
        if (response.isSuccessful) {
            emit(response.body())  // Emit successful login response
        } else {
            emit(null)  // Emit null if failed
        }
    }

    // Get the logged-in user's profile
    fun getProfile() = liveData {
        val response: Response<User> = userRepository.getProfile()
        if (response.isSuccessful) {
            emit(response.body())  // Emit profile response
        } else {
            emit(null)  // Emit null if failed
        }
    }

    // Logout the user
    fun logout() = liveData {
        val response: Response<Map<String, String>> = userRepository.logout()
        if (response.isSuccessful) {
            emit(response.body())  // Emit logout success response
        } else {
            emit(null)  // Emit null if failed
        }
    }
}