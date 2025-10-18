package com.example.capstone2.repository

import android.util.Log
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.RequestResponse
import com.example.capstone2.data.models.QueueResponse
import okhttp3.ResponseBody
import retrofit2.Response
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.StringReader


class RequestRepository(private val apiService: ApiService) {

    companion object {
        // Simple in-memory cache mapping customerID -> raw JSON string of last-good response
        private val customerRequestsCache: MutableMap<Long, String> = mutableMapOf()
        // Cache for owner requests raw JSON
        private var ownerRequestsCache: String? = null
        // Cache requestID -> payment amount to avoid repeated fetches
        private val paymentAmountCache: MutableMap<Long, Double> = mutableMapOf()
    }

    suspend fun createRequest(request: CreateRequest): Response<ResponseBody> {
        Log.d("RequestRepository", "Creating request for customerID: ${request.customerID}")
        return apiService.createRequest(request)
    }

    suspend fun updateRequest(requestId: Long, request: CreateRequest): Response<ResponseBody> {
        Log.d("RequestRepository", "Updating request ID: $requestId for customerID: ${request.customerID}")
        return apiService.updateRequest(requestId, request)
    }

    suspend fun getRequests(): Response<RequestResponse> {
        Log.d("RequestRepository", "Getting all requests")
        return apiService.getRequests()
    }

    suspend fun getOwnerRequests(): Response<RequestResponse> {
        Log.d("RequestRepository", "Getting owner requests")
        try {
            // Try the straightforward Retrofit call first. This may throw if Gson parsing fails.
            val response = apiService.getOwnerRequests()
            if (response.isSuccessful) {
                Log.d("RequestRepository", "Successfully fetched ${response.body()?.requests?.size ?: 0} owner requests")
                // Cache raw JSON of the parsed object for later fallback
                // (debug) auto-save disabled — no longer serializing/caching response JSON
            } else {
                Log.e("RequestRepository", "Failed to fetch owner requests: ${response.code()}")
            }
            return response
        } catch (e: Exception) {
            // Parsing likely failed (e.g. EOFException from Gson). Attempt to fetch raw body and leniently parse.
            Log.e("RequestRepository", "Owner requests parsing failed, attempting raw fallback", e)
        }

        // Raw fallback: fetch response body as text and attempt to recover
        try {
            val rawResp = apiService.getOwnerRequestsRaw()
            if (rawResp.isSuccessful) {
                val body = rawResp.body()?.string() ?: ""
                Log.d("RequestRepository", "Raw owner response length: ${body.length}")

                // If the server returned a pure array, wrap it in an object with "requests" key
                val trimmed = body.trim()
                val fixed = when {
                    trimmed.isEmpty() -> "{\"requests\":[] }"
                    trimmed.startsWith("[") -> "{\"requests\":$trimmed}"
                    else -> tryFixJsonObject(trimmed)
                }

                val reader = JsonReader(StringReader(fixed))
                reader.isLenient = true
                val parsed: RequestResponse = Gson().fromJson(reader, RequestResponse::class.java)
                Log.d("RequestRepository", "Parsed ${parsed.requests.size} owner requests from raw fallback")

                // (debug) previously we saved `fixed` to `ownerRequestsCache` for debugging; disabled now

                return Response.success(parsed)
            } else {
                Log.e("RequestRepository", "Raw owner fetch failed: ${rawResp.code()}")
            }
        } catch (inner: Exception) {
            Log.e("RequestRepository", "Raw fallback parsing for owner requests failed", inner)
        }

        // Try cached owner requests if available
        try {
            val cached = ownerRequestsCache
            if (!cached.isNullOrBlank()) {
                Log.d("RequestRepository", "Parsing owner requests from in-memory cache")
                val gson = Gson()
                val reader = JsonReader(StringReader(cached))
                reader.isLenient = true
                val parsed: RequestResponse = gson.fromJson(reader, RequestResponse::class.java)
                Log.d("RequestRepository", "Parsed ${parsed.requests.size} owner requests from cache")
                return Response.success(parsed)
            }
        } catch (cacheEx: Exception) {
            Log.e("RequestRepository", "Owner cache parsing failed", cacheEx)
        }

        // Nothing worked; rethrow a helpful exception
        throw Exception("Failed to fetch owner requests: parsing and fallback attempts failed")
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

    // NEW: set or update the payment amount for a request
    suspend fun setPaymentAmount(requestId: Long, amount: Double): Response<ResponseBody> {
        Log.d("RequestRepository", "Setting payment amount for request ID: $requestId to $amount")
        val body = mapOf(
            "amount" to amount
        )
        return apiService.setPaymentAmount(requestId, body)
    }

    // NEW: set milled output (kg) and payment amount in a single request body
    suspend fun setMilledAndPayment(requestId: Long, milledKg: Double, amount: Double): Response<ResponseBody> {
        Log.d("RequestRepository", "Setting milledKg=$milledKg and amount=$amount for request ID: $requestId")
        val body = mapOf(
            "milledKg" to milledKg,
            "milled_kg" to milledKg, // provide alternate key for flexible backends
            "amount" to amount,
            "paymentAmount" to amount,
            "payment_amount" to amount
        )
        return apiService.setPaymentAmount(requestId, body)
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
                            // (debug) auto-save disabled — no longer serializing/caching requests
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
                    // (debug) auto-save disabled — no longer caching fixed raw fallback
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

    /** Try to parse a numeric amount from a possibly formatted string (e.g., "₱1,234.50"). */
    private fun parseAmountStringSafe(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(Regex("[^0-9.,-]"), "").replace(",", "")
        return cleaned.toDoubleOrNull()
    }

    /** Recursively search a JSON element for a likely payment amount field. */
    private fun extractAmountRecursive(elem: JsonElement?): Double? {
        if (elem == null || elem.isJsonNull) return null
        try {
            if (elem.isJsonPrimitive) {
                val prim = elem.asJsonPrimitive
                if (prim.isNumber) return prim.asDouble
                if (prim.isString) return parseAmountStringSafe(prim.asString)
            }
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                // Common keys
                val keys = listOf(
                    "paymentAmount", "payment_amount", "amount", "price", "total", "total_amount", "payable", "balance"
                )
                for (k in keys) {
                    if (obj.has(k) && !obj.get(k).isJsonNull) {
                        val d = extractAmountRecursive(obj.get(k))
                        if (d != null) return d
                    }
                }
                // Check nested payment object
                val nests = listOf("payment", "details", "data", "attributes")
                for (nk in nests) {
                    if (obj.has(nk)) {
                        val d = extractAmountRecursive(obj.get(nk))
                        if (d != null) return d
                    }
                }
                // Also try scanning all entries
                for ((_, v) in obj.entrySet()) {
                    val d = extractAmountRecursive(v)
                    if (d != null) return d
                }
            }
            if (elem.isJsonArray) {
                for (el in elem.asJsonArray) {
                    val d = extractAmountRecursive(el)
                    if (d != null) return d
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Fetch payment amount for a single request by trying a few common endpoints and
     * extracting a numeric amount using lenient parsing.
     */
    suspend fun fetchPaymentAmount(requestId: Long): Double? {
        // Return cached if present
        paymentAmountCache[requestId]?.let { return it }

        // 1) Preferred: dedicated raw endpoint (if backend supports it)
        try {
            val resp = apiService.getPaymentAmountRaw(requestId)
            if (resp.isSuccessful) {
                val body = resp.body()?.string()?.trim()
                if (!body.isNullOrBlank()) {
                    val root = try { JsonParser.parseString(body) } catch (_: Exception) { null }
                    val amt = extractAmountRecursive(root)
                    if (amt != null && amt >= 0.0) {
                        paymentAmountCache[requestId] = amt
                        Log.d("RequestRepository", "Fetched payment amount for $requestId via dedicated endpoint = $amt")
                        return amt
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("RequestRepository", "getPaymentAmountRaw threw ${e.message}")
        }

        // 2) Fallback: try a few common endpoint patterns
        val paths = listOf(
            "api/requests/$requestId",
            "api/requests/$requestId/payment",
            "api/request/$requestId",
            "api/request/$requestId/payment",
            "api/orders/$requestId",
            "api/orders/$requestId/payment"
        )
        for (p in paths) {
            try {
                val resp = apiService.getRaw(p)
                if (!resp.isSuccessful) continue
                val body = resp.body()?.string()?.trim()
                if (body.isNullOrBlank()) continue
                val root = try { JsonParser.parseString(body) } catch (_: Exception) { null }
                val amt = extractAmountRecursive(root)
                if (amt != null && amt >= 0.0) {
                    paymentAmountCache[requestId] = amt
                    Log.d("RequestRepository", "Fetched payment amount for $requestId via $p = $amt")
                    return amt
                }
            } catch (e: Exception) {
                Log.d("RequestRepository", "fetchPaymentAmount path $p threw ${e.message}")
            }
        }
        return null
    }

    /**
     * Given a list of requests, fetch and fill missing payment amounts for those at or beyond
     * status 12 (Milling done) or 13 (Delivered). Returns a new list with updates applied.
     */
    suspend fun enrichPaymentAmounts(requests: List<Request>): List<Request> {
        if (requests.isEmpty()) return requests
        val needs = requests.filter { r ->
            (r.paymentAmount == null && (r.payment?.amount == null))
        }
        if (needs.isEmpty()) return requests
        val updates = mutableMapOf<Long, Double>()
        for (r in needs) {
            try {
                val amt = fetchPaymentAmount(r.requestID)
                if (amt != null && amt >= 0.0) updates[r.requestID] = amt
            } catch (_: Exception) {}
        }
        if (updates.isEmpty()) return requests
        return requests.map { r ->
            val a = updates[r.requestID]
            if (a != null) r.copy(statusID = r.statusID, paymentAmount = a) else r
        }
    }

    // Fetch delivery boy requests
    suspend fun getDeliveryBoyRequests(): List<Request> {
        val resp = apiService.getDeliveryBoyRequests() // use apiService instead of api
        if (resp.isSuccessful) {
            return resp.body()?.requests ?: emptyList()
        } else {
            throw Exception("Failed to fetch delivery boy requests: ${resp.code()}")
        }
    }

    suspend fun markPickupDone(requestId: Long): Response<ResponseBody> {
        Log.d("RequestRepository", "Courier marking pickup done for request ID: $requestId")
        return apiService.markPickupDone(requestId)
    }

    // New: fetch all requests filtered by status ID, if supported by backend
    suspend fun getRequestsByStatus(statusID: Int): Response<RequestResponse> {
        return apiService.getRequestsByStatus(statusID)
    }

    // New: fetch all requests filtered by status name (e.g., "pending"), if supported by backend
    suspend fun getRequestsByStatusName(status: String): Response<RequestResponse> {
        return apiService.getRequestsByStatusName(status)
    }

    suspend fun getCustomerQueue(customerID: Long): Response<QueueResponse> {
        Log.d("RequestRepository", "Getting queue for customerID: $customerID")
        return apiService.getCustomerQueue(customerID)
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
        s = s.replace(Regex(",\\s*" + java.util.regex.Pattern.quote("]")), "]")
        return s
    }

    /**
     * Make lightweight repairs to a JSON object that may be truncated or missing a closing brace.
     */
    private fun tryFixJsonObject(body: String): String {
        var s = body.trim()
        if (s.isEmpty()) return "{\"requests\":[] }"
        // If it looks like an array wrapped as a top-level value, wrap it
        if (s.startsWith("[")) return "{\"requests\":$s}"
        // Add a closing brace if missing
        if (!s.endsWith("}")) s = s + "}"
        // Remove trailing commas before a closing brace
        s = s.replace(Regex(",\\s*" + java.util.regex.Pattern.quote("}")), "}")
        // If the top-level does not contain "requests" but contains an array field, try to find it
        if (!s.contains("\"requests\"")) {
            // Best-effort: if it contains a top-level field whose value is an array, try to find it
            val arrayMatch = Regex("(\"[a-zA-Z0-9_]+\"\\s*:\\s*\\[)")
            val m = arrayMatch.find(s)
            if (m != null) {
                // wrap the entire object under requests
                return "{\"requests\":${s}}"
            }
        }
        return s
    }
}
