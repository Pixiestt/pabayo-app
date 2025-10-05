package com.example.capstone2.network

import android.content.Context
import com.example.capstone2.repository.SharedPrefManager

fun getTokenProvider(context: Context): () -> String? = {
    // Use SharedPrefManager to read the stored auth token so all callers use the
    // same preference file and key. This avoids cases where ApiClient's interceptor
    // doesn't attach Authorization because a different prefs file was read.
    SharedPrefManager.getAuthToken(context)
}
