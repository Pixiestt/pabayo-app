package com.example.capstone2.delivery

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.capstone2.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class DeliveryMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.delivery_activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationViewDelivery)

        // Default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.flFragmentDelivery, DeliveryFragmentPickups())
                .commit()
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.pickups -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.flFragmentDelivery, DeliveryFragmentPickups())
                        .commit()
                    true
                }
                R.id.deliveries -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.flFragmentDelivery, DeliveryFragmentDeliveries())
                        .commit()
                    true
                }
                R.id.logout -> {
                    showLogoutConfirmation()
                    true
                }
                else -> false
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Clear stored auth and navigate to LoginActivity
    private fun performLogout() {
        val sharedPref = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        // Use KTX edit extension for clarity
        sharedPref.edit {
            remove("auth_token")
            remove("userID")
        }

        val intent = Intent(this, com.example.capstone2.authentication.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}


