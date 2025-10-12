package com.example.capstone2.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.example.capstone2.data.models.ChangePasswordRequest
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.UpdateProfileRequest
import com.example.capstone2.data.models.User
import com.example.capstone2.repository.UserRepository
import retrofit2.Response

class UserViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val TAG = "UserViewModel"

    // Register a new user
    fun registerUser(registerRequest: RegisterRequest) = liveData {
        try {
            val response: Response<RegisterResponse> = userRepository.registerUser(registerRequest)
            if (response.isSuccessful) {
                emit(response.body())  // Emit successful response
            } else {
                emit(null)  // Emit null if failed
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerUser failed: ${e.message}")
            emit(null)
        }
    }

    // Login a user
    fun loginUser(loginRequest: LoginRequest) = liveData {
        try {
            val response: Response<LoginResponse> = userRepository.loginUser(loginRequest)
            if (response.isSuccessful) {
                emit(response.body())  // Emit successful login response
            } else {
                emit(null)  // Emit null if failed
            }
        } catch (e: Exception) {
            Log.e(TAG, "loginUser failed: ${e.message}")
            emit(null)
        }
    }

    // Get the logged-in user's profile
    fun getProfile() = liveData {
        try {
            val response: Response<User> = userRepository.getProfile()
            if (response.isSuccessful) {
                emit(response.body())  // Emit profile response
            } else {
                emit(null)  // Emit null if failed
            }
        } catch (e: Exception) {
            Log.e(TAG, "getProfile failed: ${e.message}")
            emit(null)
        }
    }

    // Update the logged-in user's profile
    fun updateProfile(request: UpdateProfileRequest) = liveData {
        try {
            val response: Response<User> = userRepository.updateProfile(request)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile failed: ${e.message}")
            emit(null)
        }
    }

    // Change the logged-in user's password
    fun changePassword(request: ChangePasswordRequest) = liveData {
        try {
            val response: Response<Map<String, String>> = userRepository.changePassword(request)
            if (response.isSuccessful) {
                emit(response.body())
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "changePassword failed: ${e.message}")
            emit(null)
        }
    }

    // Logout the user
    fun logout() = liveData {
        try {
            val response: Response<Map<String, String>> = userRepository.logout()
            if (response.isSuccessful) {
                emit(response.body())  // Emit logout success response
            } else {
                emit(null)  // Emit null if failed
            }
        } catch (e: Exception) {
            Log.e(TAG, "logout failed: ${e.message}")
            emit(null)
        }
    }
}