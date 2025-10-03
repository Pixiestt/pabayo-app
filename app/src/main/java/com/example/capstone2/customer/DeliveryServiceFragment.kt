package com.example.capstone2.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.capstone2.R
import com.example.capstone2.data.models.DeliveryService

class DeliveryServiceFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var deliveryAddressContainer: View
    private lateinit var etDeliveryLocation: EditText
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_delivery_service, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        radioGroup = view.findViewById(R.id.radioGroupDelivery)
        deliveryAddressContainer = view.findViewById(R.id.deliveryAddressContainer)
        etDeliveryLocation = view.findViewById(R.id.etDeliveryLocation)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "3/5"
        
        // Prefill from wizard data if available
        val wizard = activity.getWizardData()
        wizard.deliveryService?.let { ds ->
            when (ds) {
                DeliveryService.DELIVER_TO_LOCATION -> radioGroup.check(R.id.rbDeliverToLocation)
                DeliveryService.PICKUP_FROM_FACILITY -> radioGroup.check(R.id.rbPickupFromFacility)
            }
        }

        wizard.deliveryLocation?.let { loc ->
            etDeliveryLocation.setText(loc)
            deliveryAddressContainer.visibility = View.VISIBLE
        }

        // Initially hide/show the container and set the Next button accordingly
        when (radioGroup.checkedRadioButtonId) {
            R.id.rbDeliverToLocation -> {
                deliveryAddressContainer.visibility = View.VISIBLE
                btnNext.isEnabled = etDeliveryLocation.text.toString().trim().isNotEmpty()
            }
            R.id.rbPickupFromFacility -> {
                deliveryAddressContainer.visibility = View.GONE
                btnNext.isEnabled = true
            }
            else -> {
                deliveryAddressContainer.visibility = View.GONE
                btnNext.isEnabled = false
            }
        }

        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbDeliverToLocation -> {
                    activity.getWizardData().deliveryService = DeliveryService.DELIVER_TO_LOCATION
                    deliveryAddressContainer.visibility = View.VISIBLE
                    validateForm()
                }
                R.id.rbPickupFromFacility -> {
                    activity.getWizardData().deliveryService = DeliveryService.PICKUP_FROM_FACILITY
                    deliveryAddressContainer.visibility = View.GONE
                    etDeliveryLocation.text.clear()
                    btnNext.isEnabled = true
                }
            }
        }

        // Add text change listener to location input
        etDeliveryLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateForm()
            }
        })
        
        btnNext.setOnClickListener {
            if (radioGroup.checkedRadioButtonId == -1) {
                return@setOnClickListener
            }
            
            if (activity.getWizardData().deliveryService == DeliveryService.DELIVER_TO_LOCATION) {
                val location = etDeliveryLocation.text.toString().trim()
                if (location.isEmpty()) {
                    etDeliveryLocation.error = "Please enter your delivery address"
                    return@setOnClickListener
                }
                activity.getWizardData().deliveryLocation = location
            }
            
            activity.goToNextStep()
        }
    }

    private fun validateForm() {
        if (radioGroup.checkedRadioButtonId == R.id.rbDeliverToLocation) {
            btnNext.isEnabled = etDeliveryLocation.text.toString().trim().isNotEmpty()
        } else if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromFacility) {
            btnNext.isEnabled = true
        } else {
            btnNext.isEnabled = false
        }
    }
}
