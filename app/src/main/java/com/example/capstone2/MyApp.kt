package com.example.capstone2

import android.app.Application
import com.example.capstone2.util.NotificationUtils

class MyApp : Application() {
    companion object {
        // Global app instance to allow accessing resources from singletons
        lateinit var instance: MyApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Create notification channel early so FCM/Pusher messages have a valid channel
        NotificationUtils.createNotificationChannel(this)
    }
}
