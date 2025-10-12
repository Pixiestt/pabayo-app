package com.example.capstone2.customer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.example.capstone2.repository.SharedPrefManager

class CustomerFragmentRequest : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_request_simple, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val btnStartWizard = view.findViewById<Button>(R.id.btnStartWizard)
        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)

        tvTitle.text = "Create New Request"
        tvDescription.text = "Use our step-by-step wizard to create your rice milling request easily and quickly."
        
        btnStartWizard.setOnClickListener {
            // Check the user's account status saved in SharedPreferences
            // Use SharedPrefManager to transparently handle legacy + canonical prefs
            val status = SharedPrefManager.getUserStatus(requireContext()).trim().lowercase()

            if (status == "pending") {
                // Show blocking dialog informing user their account is not approved
                AlertDialog.Builder(requireContext())
                    .setTitle("Account not approved")
                    .setMessage("Your account is still pending approval. You cannot avail services until your account is approved.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                // Proceed to the request wizard.
                val intent = Intent(requireContext(), RequestWizardActivity::class.java)
                startActivity(intent)
            }
        }
    }
}