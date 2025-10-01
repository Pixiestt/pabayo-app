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
import com.example.capstone2.owner.OwnerMainActivity
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authentication_activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

                        when (user.roleID.toLong()) {
                            2L -> startActivity(Intent(this, OwnerMainActivity::class.java))
                            3L -> startActivity(Intent(this, CustomerMainActivity::class.java))
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
        sharedPref.edit().putString("auth_token", token).apply()
    }


    private fun saveUserID(userID: Long) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        sharedPref.edit().putLong("userID", userID).apply()
    }

    // Persist the user's account status so other parts of the app can check it.
    private fun saveUserStatus(status: String?) {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        // If API didn't send status, default to "approved" (assumption). Change this if your backend uses a different convention.
        val valueToSave = status ?: "approved"
        sharedPref.edit().putString("user_status", valueToSave).apply()
    }
}
