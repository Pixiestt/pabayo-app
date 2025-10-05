package com.example.capstone2.customer

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.capstone2.R
import com.example.capstone2.messages.MessagesFragment
import com.example.capstone2.util.NotificationUtils
import com.example.capstone2.util.PermissionUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.content.edit

class CustomerMainActivity : AppCompatActivity() {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var bottomNavigationView: BottomNavigationView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.customer_activity_main)

        // Initialize notification channel and request permission
        NotificationUtils.createNotificationChannel(this)
        PermissionUtils.requestNotificationPermission(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById(R.id.drawer_layout)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        // Handle system back (including back gestures) via OnBackPressedDispatcher to avoid deprecated onBackPressed
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // Fallback to default behavior (disable this callback and re-dispatch)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        val fragmenthome = CustomerFragmentHome()
        val fragmentrequest = CustomerFragmentRequest()
        val fragmenttrack = CustomerFragmentTrack()
        val fragmenthistory = CustomerFragmentHistory()
        val fragmentprofile = CustomerProfileFragment()

        val fragmentmessages = MessagesFragment()
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                if (f is CustomerFragmentHome) {
                    // Hide bottom nav on home fragment
                    bottomNavigationView.visibility = View.GONE
                } else {
                    // Show bottom nav on other fragments
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }, false)

        setCurrentFragment(fragmenthome)
        
        // Setup navigation drawer
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.home -> setCurrentFragment(fragmenthome)
                R.id.request -> setCurrentFragment(fragmentrequest)
                R.id.track -> setCurrentFragment(fragmenttrack)
                R.id.history -> setCurrentFragment(fragmenthistory)
                R.id.profile -> setCurrentFragment(fragmentprofile)
                R.id.logout -> {
                    // show confirmation dialog instead of immediate logout
                    showLogoutConfirmation()
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        
        // Setup bottom navigation
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> setCurrentFragment(fragmenthome)
                R.id.request -> setCurrentFragment(fragmentrequest)
                R.id.track -> setCurrentFragment(fragmenttrack)
                R.id.history -> setCurrentFragment(fragmenthistory)
                R.id.profile -> setCurrentFragment(fragmentprofile)
                R.id.messages -> setCurrentFragment(fragmentmessages)
                else -> {
                    // no-op for unknown items
                }
            }
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Permission result handling could be added here if needed
    }
    
    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }

    // Show a confirmation dialog before logging out
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
        // Use KTX edit extension which applies changes in the lambda
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