package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import android.app.DatePickerDialog
import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.capstone2.R
import com.example.capstone2.data.models.PickupService

class PickupServiceFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var etPickupLocation: EditText
    private lateinit var etPickupDate: EditText
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pickup_service, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        radioGroup = view.findViewById(R.id.radioGroupPickup)
        etPickupLocation = view.findViewById(R.id.etPickupLocation)
        etPickupDate = view.findViewById(R.id.etPickupDate)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "2/5"
        
        // Initially hide the location input and pickup date
        etPickupLocation.visibility = View.GONE
        etPickupDate.visibility = View.GONE
        btnNext.isEnabled = false

        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPickupFromLocation -> {
                    activity.getWizardData().pickupService = PickupService.PICKUP_FROM_LOCATION
                    etPickupLocation.visibility = View.VISIBLE
                    etPickupDate.visibility = View.VISIBLE
                    validateForm()
                }
                R.id.rbDropOffAtFacility -> {
                    activity.getWizardData().pickupService = PickupService.DROP_OFF_AT_FACILITY
                    etPickupLocation.visibility = View.GONE
                    etPickupLocation.text.clear()
                    etPickupDate.visibility = View.GONE
                    etPickupDate.text.clear()
                    validateForm()
                }
            }
        }
        
        // Set up date picker
        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, day)
            etPickupDate.setText(dateFormat.format(calendar.time))
            validateForm()
        }
        
        etPickupDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                datePickerListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        btnNext.setOnClickListener {
            val location = etPickupLocation.text.toString().trim()
            val pickupDate = etPickupDate.text.toString().trim()
            
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && location.isEmpty()) {
                etPickupLocation.error = "Please enter your location"
                return@setOnClickListener
            }
            
            if (pickupDate.isEmpty()) {
                etPickupDate.error = "Please select a pickup date"
                return@setOnClickListener
            }
            
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation) {
                activity.getWizardData().pickupLocation = location
            }
            
            // Save pickup date
            activity.getWizardData().pickupDate = pickupDate
            
            activity.goToNextStep()
        }
    }
    
    private fun validateForm() {
        if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation) {
            btnNext.isEnabled = etPickupLocation.text.toString().trim().isNotEmpty() &&
                    etPickupDate.text.toString().trim().isNotEmpty()
        } else {
            btnNext.isEnabled = etPickupDate.text.toString().trim().isNotEmpty()
        }
    }
}
