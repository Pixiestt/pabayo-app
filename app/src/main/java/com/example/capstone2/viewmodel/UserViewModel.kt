package com.example.capstone2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.data.models.UpdateProfileRequest
import com.example.capstone2.data.models.ChangePasswordRequest
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

    // Update the logged-in user's profile
    fun updateProfile(request: UpdateProfileRequest) = liveData {
        val response: Response<User> = userRepository.updateProfile(request)
        if (response.isSuccessful) {
            emit(response.body())
        } else {
            emit(null)
        }
    }

    // Change the logged-in user's password
    fun changePassword(request: ChangePasswordRequest) = liveData {
        val response: Response<Map<String, String>> = userRepository.changePassword(request)
        if (response.isSuccessful) {
            emit(response.body())
        } else {
            emit(null)
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