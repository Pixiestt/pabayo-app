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

    private val _updateResult = MutableLiveData<CreateRequest?>()
    val updateResult: LiveData<CreateRequest?> = _updateResult

    fun submitRequest(request: CreateRequest) {
        viewModelScope.launch {
            try {
                val response = requestRepository.createRequest(request)
                if (response.isSuccessful) {
                    // Backend may return an envelope or empty body; treat HTTP 2xx as success
                    _submitResult.postValue(request)

                    // Notify owner about new request (best-effort)
                    val notificationService = NotificationServiceFactory.getInstance(application)
                    notificationService.notifyOwnerNewRequest(request)
                } else {
                    _submitResult.postValue(null)
                }
            } catch (e: Exception) {
                _submitResult.postValue(null)
            }
        }
    }

    fun updateRequest(requestId: Long, request: CreateRequest) {
        viewModelScope.launch {
            try {
                val response = requestRepository.updateRequest(requestId, request)
                if (response.isSuccessful) {
                    // Some backends return an empty body on successful update. Use original request.
                    _updateResult.postValue(request)
                } else {
                    _updateResult.postValue(null)
                }
            } catch (e: Exception) {
                _updateResult.postValue(null)
            }
        }
    }
}