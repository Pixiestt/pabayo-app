package com.example.capstone2.owner

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class OwnerFragmentHome : Fragment(R.layout.owner_fragment_home) {
    
    private lateinit var btnViewRequests: Button
    private lateinit var btnUpdateStatus: Button
    private lateinit var btnViewHistory: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hide bottom navigation bar
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE
        
        // Find button views
        btnViewRequests = view.findViewById(R.id.btnViewRequests)
        btnUpdateStatus = view.findViewById(R.id.btnUpdateStatus)
        btnViewHistory = view.findViewById(R.id.btnViewHistory)
        
        // Set button click listeners
        btnViewRequests.setOnClickListener {
            navigateToRequests()
        }
        
        btnUpdateStatus.setOnClickListener {
            navigateToTrack()
        }
        
        btnViewHistory.setOnClickListener {
            navigateToHistory()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure bottom navigation becomes visible when leaving this fragment
        bottomNavigationView.visibility = View.VISIBLE
    }
    
    private fun navigateToRequests() {
        // Change to Request tab
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.request
    }
    
    private fun navigateToTrack() {
        // Change to Track tab
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.track
    }
    
    private fun navigateToHistory() {
        // Change to History tab
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.history
    }
}