package com.example.capstone2.network

import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.NotificationResponse
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.RequestResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/requests")
    suspend fun createRequest(@Body request: CreateRequest): Response<CreateRequest>

    @GET("api/requests")
    suspend fun getRequests(): Response<RequestResponse>

    @GET("api/owner/requests")
    suspend fun getOwnerRequests(): Response<RequestResponse>

    @GET("api/customer/{customerID}/requests")
    suspend fun getCustomerRequests(@Path("customerID") customerID: Long): Response<List<Request>>

    @POST("api/requests/{requestID}/accept")
    suspend fun acceptRequest(@Path("requestID") requestID: Long): Response<ResponseBody>

    @PUT("api/requests/{requestID}/reject")
    suspend fun rejectRequest(@Path("requestID") requestID: Long): Response<ResponseBody>

    @PUT("api/requests/{requestID}/status/{statusID}")
    suspend fun updateRequestStatus(
        @Path("requestID") requestID: Long,
        @Path("statusID") statusID: Int
    ): Response<ResponseBody>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationResponse>
} 