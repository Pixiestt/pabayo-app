package com.example.capstone2.authentication


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.models.RegisterRequest
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory

class SignUpActivity : AppCompatActivity() {

    private lateinit var userViewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.authentication_activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        userViewModel = ViewModelProvider(
            this,
            UserViewModelFactory(applicationContext) // 👈 Pass Context here
        )[UserViewModel::class.java]



        val firstNameField = findViewById<EditText>(R.id.firstnameET)
        val lastNameField = findViewById<EditText>(R.id.lastnameET)
        val contactField = findViewById<EditText>(R.id.contactnoET)
        val addressField = findViewById<EditText>(R.id.homeaddET)
        val emailField = findViewById<EditText>(R.id.emailaddET)
        val passwordField = findViewById<EditText>(R.id.passwordET)
        val confirmPasswordField = findViewById<EditText>(R.id.confirmpassET)
        val idBtn = findViewById<Button>(R.id.idBtn)
        val idPhoto = findViewById<ImageView>(R.id.idPhoto)
        val signUp = findViewById<TextView>(R.id.signUpBtn)

        val getImage =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let { idPhoto.setImageURI(it) }
            }

        idBtn.setOnClickListener {
            getImage.launch("image/*")
        }

        val backbtn = findViewById<ImageView>(R.id.backBtn)

        backbtn.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

        val signInText = findViewById<TextView>(R.id.signInTv)
        signInText.setOnClickListener{
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }



        signUp.setOnClickListener{

            if (passwordField.text.toString() != confirmPasswordField.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = RegisterRequest(
                firstName = firstNameField.text.toString(),
                lastName = lastNameField.text.toString(),
                emailAddress = emailField.text.toString(),
                contactNumber = contactField.text.toString(),
                homeAddress = addressField.text.toString(),
                IDCard = idBtn.text.toString(),
                roleID = 3,
                password = passwordField.text.toString()


            )

            userViewModel.registerUser(request).observe(this, { response ->
                if (response != null) {
                    // On success, show a message and navigate to the login screen
                    Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()

                    // Redirect to LoginActivity after successful registration
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // Handle error (registration failed)
                    Toast.makeText(this, "Registration failed! Please try again.", Toast.LENGTH_SHORT).show()
                }
            })


        }




    }
}