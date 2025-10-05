package com.example.capstone2.owner

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.capstone2.R

/**
 * Simple Owner profile fragment. Reuses the existing customer profile layout for now.
 * Add owner-specific behavior here if needed.
 */
class OwnerFragmentProfile : Fragment(R.layout.fragment_customer_profile) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Owner-specific UI wiring can be added here when required.
    }
}

