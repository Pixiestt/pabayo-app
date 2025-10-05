package com.example.capstone2.owner

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.capstone2.R
import com.example.capstone2.messages.MessagesFragment
import com.example.capstone2.util.NotificationUtils
import com.example.capstone2.util.PermissionUtils
import com.google.android.material.bottomnavigation.BottomNavigationView


class OwnerMainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigationView: BottomNavigationView

    // Make fragment instances class-level so other fragments can request navigation
    private lateinit var fragmenthome: OwnerFragmentHome
    private lateinit var fragmentrequest: OwnerFragmentRequest
    private lateinit var fragmenttrack: OwnerFragmentTrack
    private lateinit var fragmenthistory: OwnerFragmentHistory
    private lateinit var fragmentmessages: com.example.capstone2.messages.MessagesFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.owner_activity_main)

        // Set toolbar as support action bar so the overflow menu is shown
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize notification channel and request permission
        NotificationUtils.createNotificationChannel(this)
        PermissionUtils.requestNotificationPermission(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize fragments
        fragmenthome = OwnerFragmentHome()
        fragmentrequest = OwnerFragmentRequest()
        fragmenttrack = OwnerFragmentTrack()
        fragmenthistory = OwnerFragmentHistory()
        fragmentmessages = com.example.capstone2.messages.MessagesFragment()

        setCurrentFragment(fragmenthome)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)

        // Listen for fragment changes to handle bottom navigation visibility
        supportFragmentManager.registerFragmentLifecycleCallbacks(object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
                super.onFragmentViewCreated(fm, f, v, savedInstanceState)
                if (f is OwnerFragmentHome) {
                    // Bottom nav is hidden in the OwnerFragmentHome
                    bottomNavigationView.visibility = View.GONE
                } else {
                    // Show bottom nav for all other fragments
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }, false)

        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.home -> setCurrentFragment(fragmenthome)
                R.id.request -> setCurrentFragment(fragmentrequest)
                R.id.track -> setCurrentFragment(fragmenttrack)
                R.id.messages -> setCurrentFragment(fragmentmessages)
                R.id.profile -> setCurrentFragment(com.example.capstone2.owner.OwnerFragmentProfile())
            }
            true
        }
    }

    // Expose a method so child fragments can open history directly
    fun openHistory() {
        setCurrentFragment(fragmenthistory)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Permission result handling could be added here if needed
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.owner_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.logout -> {
                // Show confirmation dialog before logging out
                showLogoutConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Show confirmation dialog similar to CustomerMainActivity
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
        sharedPref.edit().remove("auth_token").remove("userID").apply()

        val intent = Intent(this, com.example.capstone2.authentication.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flFragment, fragment)
            commit()
        }
}