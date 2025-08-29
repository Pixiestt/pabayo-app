package com.example.capstone2.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.service.NotificationServiceFactory
import kotlinx.coroutines.launch

class RequestViewModel(
    private val requestRepository: RequestRepository,
    private val application: Application
) : AndroidViewModel(application) {

    private val _submitResult = MutableLiveData<CreateRequest?>()
    val submitResult: LiveData<CreateRequest?> = _submitResult

    fun submitRequest(request: CreateRequest) {
        viewModelScope.launch {
            try {
                val response = requestRepository.createRequest(request)

                if (response.isSuccessful) {
                    val createdRequest = response.body()
                    _submitResult.postValue(createdRequest)
                    
                    // Send notification to owner about new request
                    createdRequest?.let {
                        val notificationService = NotificationServiceFactory.getInstance(application)
                        notificationService.notifyOwnerNewRequest(it)
                    }
                } else {
                    _submitResult.postValue(null)
                }
            } catch (e: Exception) {
                _submitResult.postValue(null)
            }
        }
    }
}