package com.example.capstone2.repository

import android.util.Log
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.RequestResponse
import okhttp3.ResponseBody
import retrofit2.Response


class RequestRepository(private val apiService: ApiService) {

    suspend fun createRequest(request: CreateRequest): Response<CreateRequest> {
        Log.d("RequestRepository", "Creating request for customerID: ${request.customerID}")
        return apiService.createRequest(request)
    }

    suspend fun getRequests(): Response<RequestResponse> {
        Log.d("RequestRepository", "Getting all requests")
        return apiService.getRequests()
    }

    suspend fun getOwnerRequests(): Response<RequestResponse> {
        Log.d("RequestRepository", "Getting owner requests")
        val response = apiService.getOwnerRequests()
        
        if (response.isSuccessful) {
            Log.d("RequestRepository", "Successfully fetched ${response.body()?.requests?.size ?: 0} owner requests")
        } else {
            Log.e("RequestRepository", "Failed to fetch owner requests: ${response.code()}")
        }
        
        return response
    }

    suspend fun acceptRequest(requestId: Long): Response<ResponseBody> {
        Log.d("RequestRepository", "Accepting request ID: $requestId")
        return apiService.acceptRequest(requestId)
    }

    suspend fun rejectRequest(requestId: Long): Response<ResponseBody> {
        Log.d("RequestRepository", "Rejecting request ID: $requestId")
        return apiService.updateRequestStatus(requestId, 9)
    }
    
    suspend fun updateRequestStatus(requestId: Long, statusId: Int): Response<ResponseBody> {
        Log.d("RequestRepository", "Updating request ID: $requestId to status: $statusId")
        return apiService.updateRequestStatus(requestId, statusId)
    }

    suspend fun getCustomerRequests(customerID: Long): Response<List<Request>> {
        Log.d("RequestRepository", "Getting customer requests for customerID: $customerID")
        try {
            val response = apiService.getCustomerRequests(customerID)
            Log.d("RequestRepository", "Response: ${response.isSuccessful}, code: ${response.code()}")
            
            if (!response.isSuccessful) {
                Log.e("RequestRepository", "Error body: ${response.errorBody()?.string()}")
            } else {
                val requests = response.body() ?: emptyList()
                Log.d("RequestRepository", "Received ${requests.size} requests")
                
                // Log the first few requests for debugging
                requests.take(3).forEach { request ->
                    Log.d("RequestRepository", "Sample request - " +
                          "ID: ${request.requestID}, " +
                          "Status: ${request.statusID}, " +
                          "Customer: ${request.customerName}, " +
                          "Service: ${request.serviceName}")
                }
            }
            
            return response
        } catch (e: Exception) {
            Log.e("RequestRepository", "Exception in getCustomerRequests", e)
            throw e
        }
    }

}
