package com.example.capstone2.customer

// Add these imports
import android.app.DatePickerDialog
import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class PickupServiceFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var etPickupLocation: EditText
    private lateinit var etPickupDate: EditText  // Add this
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // ... existing view bindings ...
        etPickupDate = view.findViewById(R.id.etPickupDate)
        
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
        
        // Update Next button click listener
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