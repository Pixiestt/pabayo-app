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

class OwnerHistoryViewModel(private val repository: RequestRepository) : ViewModel() {

    private val _completedRequests = MutableLiveData<List<Request>>()
    val completedRequests: LiveData<List<Request>> = _completedRequests

    fun fetchCompletedRequests() {
        viewModelScope.launch {
            try {
                Log.d("OwnerHistoryViewModel", "Fetching owner requests")
                val response = repository.getOwnerRequests()
                
                Log.d("OwnerHistoryViewModel", "Response received, successful: ${response.isSuccessful}")
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body()?.requests ?: emptyList()
                    Log.d("OwnerHistoryViewModel", "Received ${requests.size} requests")
                    
                    // Filter only completed requests (status ID 8)
                    val completedRequests = requests.filter { it.statusID == 8 }
                    Log.d("OwnerHistoryViewModel", "Filtered to ${completedRequests.size} completed requests")
                    
                    _completedRequests.value = completedRequests
                } else {
                    Log.e("OwnerHistoryViewModel", "API error: ${response.code()} ${response.message()}")
                    Log.e("OwnerHistoryViewModel", "Error body: ${response.errorBody()?.string()}")
                    _completedRequests.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("OwnerHistoryViewModel", "Exception while fetching requests", e)
                e.printStackTrace()
                _completedRequests.value = emptyList()
            }
        }
    }
}

class OwnerHistoryViewModelFactory(private val repository: RequestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OwnerHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OwnerHistoryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 