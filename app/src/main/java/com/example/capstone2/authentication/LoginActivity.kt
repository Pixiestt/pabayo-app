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
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.customer.CustomerMainActivity
import com.example.capstone2.data.models.LoginRequest
import com.example.capstone2.delivery.DeliveryMainActivity
import com.example.capstone2.owner.OwnerMainActivity
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory


import com.pusher.pushnotifications.PushNotifications


import com.google.firebase.FirebaseApp


class LoginActivity : AppCompatActivity() {

    companion object {
        // TODO: replace with your actual Beams instance id
        private const val BEAMS_INSTANCE_ID = "8c4f8907-19a5-4d60-8de2-39344b7156da"
    }

    private fun getAuthToken(): String? {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        // Use a default empty string if not found, or null
        return sharedPref.getString("auth_token", null)
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

        // Ensure Firebase default app is initialized before starting PushNotifications.
        // This prevents IllegalStateException: Default FirebaseApp is not initialized
        FirebaseApp.initializeApp(this)

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
                val loginRequest = LoginRequest(email, password)

                userViewModel.loginUser(loginRequest).observe(this) { response ->
                    Log.d("LoginResponse", "Received: $response")
                    if (response != null && response.token.isNotEmpty()) {
                        val user = response.user
                        val token = response.token

                        // Save token and userID
                        saveAuthToken(token)
                        saveUserID(user.userID)
                        // Save user account status (if provided). Default to "approved" to avoid accidental blocking when API doesn't supply status.
                        saveUserStatus(user.status)

                        // -----------------------------------------------------------------
                        // NEW: Pusher Beams User Authentication
                        // -----------------------------------------------------------------
                        val authToken = getAuthToken()
                        if (authToken != null) {
                            // Make sure PushNotifications SDK is started with your instance id
                            try {
                                PushNotifications.start(applicationContext, BEAMS_INSTANCE_ID)
                            } catch (e: Exception) {
                                Log.w("PusherBeams", "PushNotifications.start threw: ${e.message}")
                            }

                            // Set the user id for Beams. Provide a BeamsCallback to receive success/failure.
                            try {
                                // Instead of using setUserId (which requires a server auth endpoint), subscribe this device to an
                                // interest named after the user. The server can publish to this interest (publishToInterests).
                                val interest = "user_${user.userID}"
                                PushNotifications.addDeviceInterest(interest)
                                Log.d("PusherBeams", "Subscribed device to interest: $interest")
                                 Log.d("PusherBeams", "Requested setUserId for Beams user ${user.userID}")
                            } catch (e: Exception) {
                                Log.e("PusherBeams", "setUserId threw: ${e.message}")
                            }
                        } else {
                            Log.e("PusherBeams", "No auth token found. Cannot authenticate Beams user.")
                        }

                        //hanggang dito yung sa beams

                        when (user.roleID) {
                            2L -> startActivity(Intent(this, OwnerMainActivity::class.java))
                            3L -> startActivity(Intent(this, CustomerMainActivity::class.java))
                            4L -> startActivity(Intent(this, DeliveryMainActivity::class.java))
                            else -> Toast.makeText(this, "Unknown role ID: ${user.roleID}", Toast.LENGTH_SHORT).show()
                        }

                        finish()
                    } else {
                        Toast.makeText(this, "Login failed: invalid credentials or token.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAuthToken(token: String?) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPref.edit { putString("auth_token", token) }
    }


    private fun saveUserID(userID: Long) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPref.edit { putLong("userID", userID) }
    }

    // Persist the user's account status so other parts of the app can check it.
    private fun saveUserStatus(status: String?) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        // If API didn't send status, default to "approved" (assumption). Change this if your backend uses a different convention.
        val valueToSave = status ?: "approved"
        sharedPref.edit { putString("user_status", valueToSave) }
    }
}
