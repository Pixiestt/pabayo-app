package com.example.capstone2.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.QueueResponse
import com.example.capstone2.repository.RequestRepository
import kotlinx.coroutines.launch

class CustomerRequestViewModel(private val repository: RequestRepository) : ViewModel() {

    private val _customerRequests = MutableLiveData<List<Request>>()
    val customerRequests: LiveData<List<Request>> = _customerRequests

    // New: explicit queue payload (pending + processing lists with original statusIDs)
    private val _queueData = MutableLiveData<QueueResponse>()
    val queueData: LiveData<QueueResponse> = _queueData

    // All requests (global list) remains unchanged
    private val _allRequests = MutableLiveData<List<Request>>()
    val allRequests: LiveData<List<Request>> = _allRequests

    private val _updateStatusResult = MutableLiveData<Boolean>()
    val updateStatusResult: LiveData<Boolean> = _updateStatusResult

    // New: fetch queue directly (no status normalization)
    fun fetchCustomerQueue(customerID: Long) {
        viewModelScope.launch {
            try {
                Log.d("CustomerViewModel", "Fetching QUEUE for customerID: $customerID")
                val resp = repository.getCustomerQueue(customerID)
                if (resp.isSuccessful && resp.body() != null) {
                    val body = resp.body()!!
                    Log.d(
                        "CustomerViewModel",
                        "Queue fetched (raw): pending=${body.pending.size} processing=${body.processing.size}"
                    )

                    // Normalize groups strictly by statusID to avoid mis-grouping from backend
                    val PENDING = setOf(4, 10)
                    val PROCESSING = setOf(5)

                    // Merge lists then partition; also drop any statuses we don't display
                    val combined = (body.pending + body.processing)
                    val normalizedPending = combined
                        .filter { it.statusID in PENDING }
                        .distinctBy { it.requestID }
                    val normalizedProcessing = combined
                        .filter { it.statusID in PROCESSING }
                        .distinctBy { it.requestID }

                    Log.d(
                        "CustomerViewModel",
                        "Queue normalized: pending=${normalizedPending.size} processing=${normalizedProcessing.size}"
                    )

                    _queueData.value = QueueResponse(
                        pending = normalizedPending,
                        processing = normalizedProcessing
                    )
                } else {
                    Log.w("CustomerViewModel", "Queue API failed: code=${resp.code()}")
                    _queueData.value = QueueResponse()
                }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Queue API exception", e)
                _queueData.value = QueueResponse()
            }
        }
    }

    // Preserve original status IDs for general customer requests
    fun fetchCustomerRequests(customerID: Long) {
        viewModelScope.launch {
            try {
                Log.d("CustomerViewModel", "Fetching requests for customerID: $customerID")
                val response = repository.getCustomerRequests(customerID)
                if (response.isSuccessful && response.body() != null) {
                    val requests = response.body() ?: emptyList()
                    Log.d("CustomerViewModel", "Received ${requests.size} requests")
                    // Keep all except completed (8) and rejected (9)
                    val filtered = requests.filter { it.statusID != 8 && it.statusID != 9 }
                    _customerRequests.value = filtered
                } else {
                    Log.e(
                        "CustomerViewModel",
                        "API error: ${response.code()} ${response.message()}"
                    )
                    _customerRequests.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Exception while fetching requests", e)
                _customerRequests.value = emptyList()
            }
        }
    }

    fun fetchAllRequests() {
        viewModelScope.launch {
            try {
                Log.d("CustomerViewModel", "Fetching ALL requests for queue view (full list)")
                try {
                    val respAll = repository.getRequests()
                    if (respAll.isSuccessful) {
                        val list = respAll.body()?.requests ?: emptyList()
                        Log.d("CustomerViewModel", "getRequests returned ${list.size}")
                        if (list.isNotEmpty()) {
                            _allRequests.value = list
                            return@launch
                        }
                    } else {
                        Log.w("CustomerViewModel", "getRequests failed: ${respAll.code()}")
                    }
                } catch (ignored: Exception) {
                    Log.w("CustomerViewModel", "getRequests threw: ${ignored.message}")
                }

                try {
                    val ownerResp = repository.getOwnerRequests()
                    if (ownerResp.isSuccessful) {
                        val list = ownerResp.body()?.requests ?: emptyList()
                        Log.d("CustomerViewModel", "getOwnerRequests returned ${list.size}")
                        if (list.isNotEmpty()) {
                            _allRequests.value = list
                            return@launch
                        }
                    } else {
                        Log.w("CustomerViewModel", "getOwnerRequests failed: ${ownerResp.code()}")
                    }
                } catch (ignored: Exception) {
                    Log.w("CustomerViewModel", "getOwnerRequests threw: ${ignored.message}")
                }

                _allRequests.value = emptyList()
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Exception while fetching all requests", e)
                _allRequests.value = emptyList()
            }
        }
    }

    fun markRequestAsComplete(requestID: Long) {
        viewModelScope.launch {
            try {
                val response = repository.updateRequestStatus(requestID, 8)
                _updateStatusResult.value = response.isSuccessful
            } catch (e: Exception) {
                Log.e("CustomerViewModel", "Error marking request as complete", e)
                _updateStatusResult.value = false
            }
        }
    }
}
