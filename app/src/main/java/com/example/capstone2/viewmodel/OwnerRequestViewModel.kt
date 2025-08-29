package com.example.capstone2.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.service.NotificationServiceFactory
import kotlinx.coroutines.launch

class OwnerRequestViewModel(
    private val repository: RequestRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _ownerRequests = MutableLiveData<List<Request>>()
    val ownerRequests: LiveData<List<Request>> = _ownerRequests
    
    // Track loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun fetchOwnerRequests() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d("OwnerRequestViewModel", "Fetching owner requests")
                val response = repository.getOwnerRequests()
                
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body()!!.requests
                    Log.d("OwnerRequestViewModel", "Fetched ${requests.size} requests successfully")
                    _ownerRequests.value = requests
                } else {
                    Log.e("OwnerRequestViewModel", "Failed to fetch requests: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error fetching requests", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun acceptRequest(request: Request, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("OwnerRequestViewModel", "Accepting request ID: ${request.requestID}")
                val response = repository.acceptRequest(request.requestID)
                
                if (response.isSuccessful) {
                    Log.d("OwnerRequestViewModel", "Successfully accepted request")
                    
                    // Update the status of the request in the list
                    val updatedList = _ownerRequests.value?.map { existingRequest ->
                        if (existingRequest.requestID == request.requestID) {
                            existingRequest.copy(statusID = 10)  // Set status to Accepted (10)
                        } else {
                            existingRequest
                        }
                    }
                    
                    // Update the LiveData with the modified list
                    updatedList?.let {
                        _ownerRequests.value = it
                    }
                    
                    // Send notification to customer
                    val notificationService = NotificationServiceFactory.getInstance(getApplication())
                    notificationService.notifyCustomerRequestAccepted(request)
                    
                    onSuccess()
                } else {
                    Log.e("OwnerRequestViewModel", "Failed to accept request: ${response.code()} ${response.message()}")
                    onError("Failed to accept request. Please try again.")
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error accepting request", e)
                onError("Error: ${e.localizedMessage}")
            }
        }
    }

    fun rejectRequest(request: Request, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("OwnerRequestViewModel", "Rejecting request ID: ${request.requestID}")
                val response = repository.rejectRequest(request.requestID)
                
                if (response.isSuccessful) {
                    Log.d("OwnerRequestViewModel", "Successfully rejected request")
                    
                    // Remove the rejected request from the list or update its status
                    val updatedList = _ownerRequests.value?.filter { existingRequest ->
                        existingRequest.requestID != request.requestID
                    }
                    
                    // Update the LiveData with the modified list
                    updatedList?.let {
                        _ownerRequests.value = it
                    }
                    
                    // Send status update notification (rejected)
                    val notificationService = NotificationServiceFactory.getInstance(getApplication())
                    notificationService.notifyCustomerStatusUpdate(request, 9) // 9 = Rejected
                    
                    onSuccess()
                } else {
                    Log.e("OwnerRequestViewModel", "Failed to reject request: ${response.code()} ${response.message()}")
                    onError("Failed to reject request. Please try again.")
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error rejecting request", e)
                onError("Error: ${e.localizedMessage}")
            }
        }
    }

    fun updateStatus(requestID: Long, newStatusID: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("OwnerRequestViewModel", "Updating request ID: $requestID to status: $newStatusID")
                val response = repository.updateRequestStatus(requestID, newStatusID)
                
                if (response.isSuccessful) {
                    Log.d("OwnerRequestViewModel", "Successfully updated request status")
                    
                    // Update the status of the request in the list
                    val updatedList = _ownerRequests.value?.map { existingRequest ->
                        if (existingRequest.requestID == requestID) {
                            existingRequest.copy(statusID = newStatusID)
                        } else {
                            existingRequest
                        }
                    }
                    
                    // Update the LiveData with the modified list
                    updatedList?.let {
                        _ownerRequests.value = it
                    }
                    
                    // Find the request in our list
                    val request = _ownerRequests.value?.find { it.requestID == requestID }
                    
                    // Send notification to customer about status update
                    request?.let {
                        val notificationService = NotificationServiceFactory.getInstance(getApplication())
                        notificationService.notifyCustomerStatusUpdate(it, newStatusID)
                    }
                    
                    onSuccess()
                } else {
                    Log.e("OwnerRequestViewModel", "Failed to update status: ${response.code()} ${response.message()}")
                    onError("Failed to update status")
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error updating request status", e)
                onError("Error: ${e.localizedMessage}")
            }
        }
    }
    
    /**
     * Get text description for status ID
     */
    private fun getStatusText(statusId: Int): String {
        return when (statusId) {
            1 -> "Subject for approval"
            2 -> "Delivery boy pickup"
            3 -> "Waiting for customer drop off"
            4 -> "Pending"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer pickup"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
            else -> "Unknown status"
        }
    }
}
