package com.example.capstone2.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import kotlinx.coroutines.launch

class CustomerHistoryViewModel(private val repository: RequestRepository) : ViewModel() {

    private val _completedRequests = MutableLiveData<List<Request>>()
    val completedRequests: LiveData<List<Request>> = _completedRequests

    fun fetchCompletedRequests(customerID: Long) {
        viewModelScope.launch {
            try {
                Log.d("CustomerHistoryViewModel", "Fetching requests for customerID: $customerID")
                val response = repository.getCustomerRequests(customerID)
                
                Log.d("CustomerHistoryViewModel", "Response received, successful: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body() ?: emptyList()
                    Log.d("CustomerHistoryViewModel", "Received ${requests.size} requests")
                    
                    // Filter only completed requests (status ID 8)
                    val completedRequests = requests.filter { it.statusID == 8 }
                    Log.d("CustomerHistoryViewModel", "Filtered to ${completedRequests.size} completed requests")
                    
                    _completedRequests.value = completedRequests
                } else {
                    Log.e("CustomerHistoryViewModel", "API error: ${response.code()} ${response.message()}")
                    Log.e("CustomerHistoryViewModel", "Error body: ${response.errorBody()?.string()}")
                    _completedRequests.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("CustomerHistoryViewModel", "Exception while fetching requests", e)
                e.printStackTrace()
                _completedRequests.value = emptyList()
            }
        }
    }
}

class CustomerHistoryViewModelFactory(private val repository: RequestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 