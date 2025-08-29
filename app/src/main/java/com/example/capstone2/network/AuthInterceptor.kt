package com.example.capstone2.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()

        val requestBuilder = original.newBuilder()
        token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $token")
            Log.d("AuthInterceptor", "Attaching token: Bearer $token")
        }



        return chain.proceed(requestBuilder.build())
    }
}
