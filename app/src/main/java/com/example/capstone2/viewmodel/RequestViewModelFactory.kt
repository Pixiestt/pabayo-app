package com.example.capstone2.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.repository.RequestRepository

class RequestViewModelFactory(
    private val requestRepository: RequestRepository,
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RequestViewModel::class.java)) {
            return RequestViewModel(requestRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}