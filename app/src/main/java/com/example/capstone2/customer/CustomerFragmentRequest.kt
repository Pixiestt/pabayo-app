package com.example.capstone2.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.capstone2.R

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
            val intent = Intent(requireContext(), RequestWizardActivity::class.java)
            startActivity(intent)
        }
    }
}