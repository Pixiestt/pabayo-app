package com.example.capstone2.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import kotlinx.coroutines.launch

class CustomerRequestViewModel(private val repository: RequestRepository) : ViewModel() {

    private val _customerRequests = MutableLiveData<List<Request>>()
    val customerRequests: LiveData<List<Request>> = _customerRequests
    
    // For tracking request status update results
    private val _updateStatusResult = MutableLiveData<Boolean>()
    val updateStatusResult: LiveData<Boolean> = _updateStatusResult

    fun fetchCustomerRequests(customerID: Long) {
        viewModelScope.launch {
            try {
                Log.d("CustomerViewModel", "Fetching requests for customerID: $customerID")
                val response = repository.getCustomerRequests(customerID)
                
                Log.d("CustomerViewModel", "Response received, successful: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body() ?: emptyList()
                    Log.d("CustomerViewModel", "Received ${requests.size} requests")
                    
                    // Log each request for debugging
                    requests.forEach { request ->
                        Log.d("CustomerViewModel", "Request: ID=${request.requestID}, " +
                              "Status=${request.statusID}, " +
                              "ServiceID=${request.serviceID}, " +
                              "Service=${request.serviceName}")
                    }
                    
                    // Keep all status types except completed (8) and rejected (9)
                    val filteredRequests = requests.filter { 
                        try {
                            val statusId = it.statusID.toInt()
                            statusId != 8 && statusId != 9
                        } catch (e: NumberFormatException) {
                            // If statusID isn't a valid integer, include it anyway
                            Log.e("CustomerViewModel", "Invalid statusID: ${it.statusID}")
                            true
                        }
                    }
                    
                    Log.d("CustomerViewModel", "After filtering: ${filteredRequests.size} requests")
                    _customerRequests.value = filteredRequests
                } else {
                    Log.e("CustomerViewModel", "API error: ${response.code()} ${response.message()}")
                    Log.e("CustomerViewModel", "Error body: ${response.errorBody()?.string()}")
                    _customerRequests.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Exception while fetching requests", e)
                e.printStackTrace()
                _customerRequests.value = emptyList()
            }
        }
    }
    
    fun markRequestAsComplete(requestID: Long) {
        viewModelScope.launch {
            try {
                val response = repository.updateRequestStatus(requestID, 8) // Status 8 = Completed
                _updateStatusResult.value = response.isSuccessful
                
                if (response.isSuccessful) {
                    // Get customerID from preferences or some other reliable source in a real app
                    // For now, let's refresh all customer requests
                    // This is a hack - in reality you should track the current user's ID
                    val customerRequests = _customerRequests.value
                    if (!customerRequests.isNullOrEmpty()) {
                        val firstRequest = customerRequests.firstOrNull()
                        firstRequest?.let {
                            fetchCustomerRequests(it.customerID.toLong())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error marking request as complete", e)
                _updateStatusResult.value = false
            }
        }
    }
} 