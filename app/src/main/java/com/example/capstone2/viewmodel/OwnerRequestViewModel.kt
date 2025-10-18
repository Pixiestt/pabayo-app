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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
                    // Enrich with payment amounts when missing
                    val enriched = try {
                        repository.enrichPaymentAmounts(requests)
                    } catch (e: Exception) {
                        Log.w("OwnerRequestViewModel", "enrichPaymentAmounts failed: ${e.message}")
                        requests
                    }
                    _ownerRequests.value = enriched
                } else {
                    Log.e("OwnerRequestViewModel", "Failed to fetch requests: ${response.code()} ${response.message()}")
                    _ownerRequests.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error fetching requests", e)
                _ownerRequests.value = emptyList()
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
    
    // NEW: Set payment amount then update status in one flow
    fun setPaymentAmountThenUpdateStatus(
        requestID: Long,
        amount: Double,
        newStatusID: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("OwnerRequestViewModel", "Setting amount=$amount for request=$requestID then updating status to $newStatusID")
                val setResp = repository.setPaymentAmount(requestID, amount)
                if (!setResp.isSuccessful) {
                    val msg = buildServerErrorMessage(setResp, fallback = "Failed to set payment amount (${setResp.code()})")
                    onError(msg)
                    return@launch
                }

                // Optimistically update local list with the new amount so UI reflects immediately
                _ownerRequests.value = _ownerRequests.value?.map { existing ->
                    if (existing.requestID == requestID) existing.copy(paymentAmount = amount) else existing
                }

                // Now update status
                val updResp = repository.updateRequestStatus(requestID, newStatusID)
                if (updResp.isSuccessful) {
                    // Update cached list
                    val updatedList = _ownerRequests.value?.map { existingRequest ->
                        if (existingRequest.requestID == requestID) existingRequest.copy(statusID = newStatusID) else existingRequest
                    }
                    updatedList?.let { _ownerRequests.value = it }

                    // Notify
                    val request = _ownerRequests.value?.find { it.requestID == requestID }
                    request?.let {
                        val notificationService = NotificationServiceFactory.getInstance(getApplication())
                        notificationService.notifyCustomerStatusUpdate(it, newStatusID)
                    }
                    onSuccess()
                } else {
                    val msg = buildServerErrorMessage(updResp, fallback = "Failed to update status after setting amount (${updResp.code()})")
                    onError(msg)
                }
            } catch (e: Exception) {
                Log.e("OwnerRequestViewModel", "Error setting amount then updating status", e)
                onError("${e.localizedMessage}")
            }
        }
    }

    // Build a concise message from server error body if available
    private fun buildServerErrorMessage(resp: retrofit2.Response<*>, fallback: String): String {
        return try {
            val err = resp.errorBody()?.string()?.trim()
            if (!err.isNullOrEmpty()) {
                // Try to extract a top-level "message" if it's JSON
                val msg = try {
                    val obj = com.google.gson.JsonParser.parseString(err).asJsonObject
                    when {
                        obj.has("message") -> obj.get("message").asString
                        obj.has("error") -> obj.get("error").asString
                        else -> null
                    }
                } catch (_: Exception) { null }
                msg?.takeIf { it.isNotBlank() } ?: ("${fallback}: ${err.take(200)}")
            } else fallback
        } catch (_: Exception) {
            fallback
        }
    }
}
