package com.example.capstone2.customer

import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.example.capstone2.data.models.RequestWizardData
import java.text.SimpleDateFormat
import java.util.Locale

class ContactDetailsFragment : Fragment() {
    
    private lateinit var etCustomerName: EditText
    private lateinit var etContactNumber: EditText
    private lateinit var etComment: EditText
    private lateinit var btnSubmit: Button
    private lateinit var tvStepProgress: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contact_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        etCustomerName = view.findViewById(R.id.etCustomerName)
        etContactNumber = view.findViewById(R.id.etContactNumber)
        etComment = view.findViewById(R.id.etComment)
        btnSubmit = view.findViewById(R.id.btnSubmit)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)

        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "5/5"

        // Prefill from wizard data if present
        val wizard = activity.getWizardData()
        wizard.customerName?.let { etCustomerName.setText(it) }
        wizard.contactNumber?.let { etContactNumber.setText(it) }
        wizard.comment?.let { etComment.setText(it) }

        btnSubmit.setOnClickListener {
            if (validateForm()) {
                val activity = requireActivity() as RequestWizardActivity
                activity.getWizardData().customerName = etCustomerName.text.toString().trim()
                activity.getWizardData().contactNumber = etContactNumber.text.toString().trim()
                activity.getWizardData().comment = etComment.text.toString().trim()
                
                activity.submitRequest()
            }
        }
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Validate customer name
        if (TextUtils.isEmpty(etCustomerName.text)) {
            etCustomerName.error = "Please enter your name"
            isValid = false
        }
        
        // Validate contact number
        if (TextUtils.isEmpty(etContactNumber.text)) {
            etContactNumber.error = "Please enter your contact number"
            isValid = false
        } else if (etContactNumber.text.toString().length < 10) {
            etContactNumber.error = "Please enter a valid contact number"
            isValid = false
        }
        
        return isValid
    }
}
