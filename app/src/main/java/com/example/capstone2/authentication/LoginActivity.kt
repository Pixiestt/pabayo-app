package com.example.capstone2.authentication

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.customer.CustomerMainActivity
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.delivery.DeliveryMainActivity
import com.example.capstone2.owner.OwnerMainActivity
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory


import com.pusher.pushnotifications.PushNotifications



import com.google.firebase.FirebaseApp
import com.example.capstone2.util.NotificationUtils


class LoginActivity : AppCompatActivity() {

    companion object {
        // TODO: replace with your actual Beams instance id
        private const val BEAMS_INSTANCE_ID = "8c4f8907-19a5-4d60-8de2-39344b7156da"
    }

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authentication_activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Try to initialize Firebase and notification channel defensively (some devices/configs may fail)
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.w("LoginActivity", "FirebaseApp.initializeApp threw: ${e.message}")
        }

        try {
            NotificationUtils.createNotificationChannel(this)
        } catch (e: Exception) {
            Log.w("LoginActivity", "createNotificationChannel threw: ${e.message}")
        }

        // Note: We start PushNotifications right after a successful login (so we have an auth token).
        // If you prefer, move PushNotifications.start(...) into your Application subclass so it runs once.

        userViewModel = ViewModelProvider(
            this,
            UserViewModelFactory(applicationContext)
        )[UserViewModel::class.java]

        val emailEditText = findViewById<EditText>(R.id.emailAddET)
        val passwordEditText = findViewById<EditText>(R.id.passET)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val signupTextView = findViewById<TextView>(R.id.signupTv)
        val forgotPassTextView = findViewById<TextView>(R.id.forgotpassTv)

        // Password toggle button
        val togglePassBtn = findViewById<ImageButton>(R.id.togglePassBtn)
        var isPasswordVisible = false
        // Ensure password is masked initially
        passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
        passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        togglePassBtn.contentDescription = "Show password"

        togglePassBtn.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // show password
                passwordEditText.transformationMethod = null
                togglePassBtn.setImageResource(android.R.drawable.ic_menu_view)
                togglePassBtn.contentDescription = "Hide password"
            } else {
                // hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                togglePassBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                togglePassBtn.contentDescription = "Show password"
            }
            // keep focus and cursor at the end
            passwordEditText.requestFocus()
            passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
        }

        signupTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPassTextView.setOnClickListener {
            startActivity(Intent(this, PassRecoveryActivity::class.java))
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                val loginRequest = LoginRequest(emailAddress = email, email = email, password = password)

                userViewModel.loginUser(loginRequest).observe(this) { response ->
                    try {
                        Log.d("LoginResponse", "Received: $response")
                        if (response == null) {
                            Toast.makeText(this, "Login failed: invalid credentials or token.", Toast.LENGTH_SHORT).show()
                            return@observe
                        }

                        val user = response.user
                        val token = response.token

                        if (token.isNullOrEmpty()) {
                            Toast.makeText(this, "Login failed: empty token.", Toast.LENGTH_SHORT).show()
                            return@observe
                        }

                        // Save token and userID using the centralized SharedPrefManager (robust)
                        try {
                            SharedPrefManager.saveAuthToken(this, token)
                        } catch (e: Exception) {
                            Log.w("LoginActivity", "Failed to save auth token: ${e.message}")
                        }

                        try {
                            user?.let {
                                SharedPrefManager.saveUserId(this, it.userID)
                                // Save user account status (if provided). Default to "approved" to avoid accidental blocking when API doesn't supply status.
                                SharedPrefManager.run {
                                    // reuse existing preference file
                                    val p = getSharedPreferences("capstone_prefs", MODE_PRIVATE)
                                    p.edit().putString("user_status", it.status ?: "approved").apply()
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("LoginActivity", "Failed to save user id/status: ${e.message}")
                        }

                        // -----------------------------------------------------------------
                        // NEW: Pusher Beams User Authentication (defensive)
                        // -----------------------------------------------------------------
                        val authToken = SharedPrefManager.getAuthToken(this)
                        if (!authToken.isNullOrBlank()) {
                            try {
                                PushNotifications.start(applicationContext, BEAMS_INSTANCE_ID)
                            } catch (e: Exception) {
                                Log.w("PusherBeams", "PushNotifications.start threw: ${e.message}")
                            }

                            try {
                                val interest = "user_${user?.userID ?: "unknown"}"
                                PushNotifications.addDeviceInterest(interest)
                                Log.d("PusherBeams", "Subscribed device to interest: $interest")
                            } catch (e: Exception) {
                                Log.e("PusherBeams", "addDeviceInterest threw: ${e.message}")
                            }
                        } else {
                            Log.e("PusherBeams", "No auth token found. Cannot authenticate Beams user.")
                        }

                        // Navigate based on role, handle nulls defensively
                        val role = user?.roleID ?: -1L
                        when (role) {
                            2L -> startActivity(Intent(this, OwnerMainActivity::class.java))
                            3L -> startActivity(Intent(this, CustomerMainActivity::class.java))
                            4L -> startActivity(Intent(this, DeliveryMainActivity::class.java))
                            else -> Toast.makeText(this, "Unknown or missing role ID: $role", Toast.LENGTH_SHORT).show()
                        }

                        finish()
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Exception processing login response: ${e.message}")
                        Toast.makeText(this, "An error occurred while logging in.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
