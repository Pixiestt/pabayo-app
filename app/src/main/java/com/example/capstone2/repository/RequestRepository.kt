package com.example.capstone2.repository

import android.util.Log
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.RequestResponse
import okhttp3.ResponseBody
import retrofit2.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import java.io.StringReader


class RequestRepository(private val apiService: ApiService) {

    companion object {
        // Simple in-memory cache mapping customerID -> raw JSON string of last-good response
        private val customerRequestsCache: MutableMap<Long, String> = mutableMapOf()
    }

    suspend fun createRequest(request: CreateRequest): Response<CreateRequest> {
        Log.d("RequestRepository", "Creating request for customerID: ${request.customerID}")
        return apiService.createRequest(request)
    }

    suspend fun updateRequest(requestId: Long, request: CreateRequest): Response<CreateRequest> {
        Log.d("RequestRepository", "Updating request ID: $requestId for customerID: ${request.customerID}")
        return apiService.updateRequest(requestId, request)
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
        val maxAttempts = 2
        var attempt = 0
        var lastException: Exception? = null
        val gson = Gson()
        try {
            while (attempt < maxAttempts) {
                try {
                    attempt++
                    val response = apiService.getCustomerRequests(customerID)
                    Log.d("RequestRepository", "Response: ${response.isSuccessful}, code: ${response.code()}")
                    if (response.isSuccessful) {
                        val requests = response.body() ?: emptyList()
                        Log.d("RequestRepository", "Received ${requests.size} requests")
                        // Cache raw JSON for later fallback
                        try {
                            val raw = gson.toJson(requests)
                            companionApplyCache(customerID, raw)
                        } catch (ignore: Exception) {
                            Log.w("RequestRepository", "Failed to cache response", ignore)
                        }

                        // Log a few samples
                        requests.take(3).forEach { request ->
                            Log.d("RequestRepository", "Sample request - " +
                                  "ID: ${request.requestID}, " +
                                  "Status: ${request.statusID}, " +
                                  "Customer: ${request.customerName}, " +
                                  "Service: ${request.serviceName}")
                        }
                        return response
                    } else {
                        Log.e("RequestRepository", "Error body: ${response.errorBody()?.string()}")
                        // If server returned an error, break to attempt fallback
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                    Log.e("RequestRepository", "Attempt $attempt failed", e)
                    // retry loop will continue up to maxAttempts
                }
            }

            // If here, all attempts failed or server returned non-success. Try raw fallback first.
        } catch (outer: Exception) {
            Log.e("RequestRepository", "Unexpected error in fetch loop", outer)
        }
        // RAW fallback: attempt to fetch raw body and lenient-parse it
        try {
            Log.d("RequestRepository", "Attempting raw fallback for customer requests")
            val rawResp = apiService.getCustomerRequestsRaw(customerID)
            if (rawResp.isSuccessful) {
                val body = rawResp.body()?.string() ?: ""
                Log.d("RequestRepository", "Raw response length: ${body.length}")
                val fixed = tryFixJsonArray(body)
                val listType = object : TypeToken<List<Request>>() {}.type
                // Use a lenient JsonReader
                val reader = JsonReader(StringReader(fixed))
                reader.isLenient = true
                val parsed: List<Request> = gson.fromJson(reader, listType)
                Log.d("RequestRepository", "Parsed ${parsed.size} requests from raw fallback")
                // cache raw string
                try {
                    companionApplyCache(customerID, fixed)
                } catch (_: Exception) {}
                return Response.success(parsed)
            } else {
                Log.e("RequestRepository", "Raw fetch failed: ${rawResp.code()}")
            }
        } catch (inner: Exception) {
            Log.e("RequestRepository", "Raw fallback parsing failed", inner)
        }

        // If all network attempts and raw fallback failed, try returning cached copy if available
        try {
            val cached = customerRequestsCache[customerID]
            if (!cached.isNullOrBlank()) {
                Log.d("RequestRepository", "Parsing from in-memory cache for customerID=$customerID")
                val reader = JsonReader(StringReader(cached))
                reader.isLenient = true
                val listType = object : TypeToken<List<Request>>() {}.type
                val parsed: List<Request> = gson.fromJson(reader, listType)
                Log.d("RequestRepository", "Parsed ${parsed.size} requests from cache")
                return Response.success(parsed)
            }
        } catch (cacheEx: Exception) {
            Log.e("RequestRepository", "Cache parsing failed", cacheEx)
        }

        // Nothing worked; throw the last network exception if present, otherwise a generic exception
        lastException?.let { throw it }
        throw Exception("Failed to fetch customer requests and no cache available")
    }

    private fun companionApplyCache(customerID: Long, rawJson: String) {
        try {
            customerRequestsCache[customerID] = rawJson
        } catch (_: Exception) {
            // ignore caching failures
        }
    }

    /**
     * Try to recover from truncated/malformed JSON arrays by simple heuristics:
     * - Trim surrounding whitespace
     * - If missing trailing ']', append it
     * - Remove trailing commas before the closing bracket
     */
    private fun tryFixJsonArray(body: String): String {
        var s = body.trim()
        if (s.isEmpty()) return "[]"
        // Remove any unexpected control characters at the end
        // Quick heuristic: if it doesn't end with ']' try appending it
        if (!s.endsWith("]")) {
            s = s + "]"
        }
        // Remove trailing commas before a closing bracket: ", ]" -> "]"
        // Use a properly escaped regex string: match comma + optional whitespace + closing bracket
        s = s.replace(Regex(",\\s*]"), "]")
        return s
    }

}
