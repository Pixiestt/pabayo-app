package com.example.capstone2.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
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
            UserViewModelFactory(applicationContext)  // 👈 fix: pass context
        )[UserViewModel::class.java]

        val emailEditText = findViewById<EditText>(R.id.emailAddET)
        val passwordEditText = findViewById<EditText>(R.id.passET)
        val loginButton = findViewById<Button>(R.id.loginBtn)
        val signupTextView = findViewById<TextView>(R.id.signupTv)
        val forgotPassTextView = findViewById<TextView>(R.id.forgotpassTv)

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

                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()

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
}
