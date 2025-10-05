package com.example.capstone2.data.api

import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.data.models.LoginResponse
import com.example.capstone2.data.models.NotificationResponse
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.data.models.RegisterResponse
import com.example.capstone2.data.models.Request
import com.example.capstone2.data.models.RequestResponse
import com.example.capstone2.data.models.User
import com.example.capstone2.data.models.Message
import com.example.capstone2.data.models.SendMessageRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/requests")
    suspend fun createRequest(@Body request: CreateRequest): Response<CreateRequest>

    @GET("api/requests")
    suspend fun getRequests(): Response<RequestResponse>

    @GET("api/owner/requests")
    suspend fun getOwnerRequests(): Response<RequestResponse>

    // Raw endpoint to fetch owner requests body as ResponseBody so caller can attempt lenient parsing
    @GET("api/owner/requests")
    suspend fun getOwnerRequestsRaw(): Response<okhttp3.ResponseBody>

    @GET("api/profile")
    suspend fun getProfile(): Response<User>

    // Update user profile (fields editable by user during sign-up)
    @PUT("api/profile")
    suspend fun updateProfile(@Body request: com.example.capstone2.data.models.UpdateProfileRequest): Response<User>

    // Change password endpoint
    @PUT("api/profile/password")
    suspend fun changePassword(@Body request: com.example.capstone2.data.models.ChangePasswordRequest): Response<Map<String, String>>

    @POST("api/logout")
    suspend fun logout(): Response<Map<String, String>>

    @POST("api/requests/{requestID}/accept")
    suspend fun acceptRequest(@Path("requestID") requestID: Long): Response<ResponseBody>

    @PUT("api/requests/{requestID}/reject")
    suspend fun rejectRequest(@Path("requestID") requestID: Long): Response<ResponseBody>

    @PUT("api/requests/{requestID}/status/{statusID}")
    suspend fun updateRequestStatus(
        @Path("requestID") requestID: Long,
        @Path("statusID") statusID: Int
    ): Response<ResponseBody>

    @GET("api/customer/{customerID}/requests")
    suspend fun getCustomerRequests(@Path("customerID") customerID: Long): Response<List<Request>>

    // Raw endpoint to fetch body as ResponseBody so caller can attempt lenient parsing
    @GET("api/customer/{customerID}/requests")
    suspend fun getCustomerRequestsRaw(@Path("customerID") customerID: Long): Response<okhttp3.ResponseBody>

    @GET("api/notifications")
    suspend fun getNotifications(): Response<NotificationResponse>

    @PUT("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: Long): Response<ResponseBody>

    @DELETE("api/notifications/{notificationId}")
    suspend fun deleteNotification(@Path("notificationId") notificationId: Long): Response<ResponseBody>

    // Update a request (used for editing existing requests)
    @PUT("api/requests/{requestID}")
    suspend fun updateRequest(
        @Path("requestID") requestID: Long,
        @Body request: CreateRequest
    ): Response<CreateRequest>

    // Chat endpoints
    @GET("api/messages/conversation")
    suspend fun getConversation(
        @Query("conversationID") conversationID: String? = null,
        @Query("otherUserID") otherUserID: Long? = null,
        @Query("limit") limit: Int = 50,
        @Query("beforeMessageID") beforeMessageID: Int? = null
    ): Response<okhttp3.ResponseBody>

    // Fetch list of conversations for current user (envelope or array)
    @GET("api/messages/conversations")
    suspend fun getConversations(): Response<okhttp3.ResponseBody>

    @POST("api/messages/send")
    suspend fun sendMessageRaw(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<okhttp3.ResponseBody>

    // Fetch a user's profile by id. Assumption: backend provides an endpoint at /api/users/{userID}
    // that returns a JSON matching the `User` data class. If your backend uses a different
    // path, update this accordingly.
    @GET("api/users/{userID}")
    suspend fun getUser(@Path("userID") userID: Long): Response<User>

    // Generic raw GET by relative URL (useful when server uses varying user endpoints)
    @GET
    suspend fun getRaw(@Url url: String): Response<okhttp3.ResponseBody>
}