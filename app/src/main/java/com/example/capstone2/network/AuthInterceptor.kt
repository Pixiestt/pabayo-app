package com.example.capstone2.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val requestBuilder = original.newBuilder()
        // Always advertise we accept JSON so Laravel returns JSON errors instead of HTML redirects
        requestBuilder.addHeader("Accept", "application/json")
        if (token.isNullOrBlank()) {
            Log.d("AuthInterceptor", "No auth token available; request will be unauthenticated")
        } else {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "Attaching Authorization header")
        }

        return chain.proceed(requestBuilder.build())
    }
}
