package com.example.capstone2.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.app.DatePickerDialog
import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.capstone2.R
import com.example.capstone2.data.models.PickupService
import androidx.core.widget.addTextChangedListener

class PickupServiceFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var pickupDetailsContainer: View
    private lateinit var etPickupLocation: EditText
    private lateinit var etPickupDate: EditText
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private val TAG = "PickupServiceFragment"

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
        pickupDetailsContainer = view.findViewById(R.id.pickupDetailsContainer)
        etPickupLocation = view.findViewById(R.id.etPickupLocation)
        etPickupDate = view.findViewById(R.id.etPickupDate)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        
        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "2/5"
        
        // Prefill from wizard data if present
        val wizard = activity.getWizardData()
        wizard.pickupService?.let { ps ->
            when (ps) {
                PickupService.PICKUP_FROM_LOCATION -> radioGroup.check(R.id.rbPickupFromLocation)
                PickupService.DROP_OFF_AT_FACILITY -> radioGroup.check(R.id.rbDropOffAtFacility)
            }
        }

        // If wizard has stored pickup location/date, show container and set values
        var hasPrefill = false
        wizard.pickupLocation?.let { loc ->
            etPickupLocation.setText(loc)
            hasPrefill = true
        }
        wizard.pickupDate?.let { pd ->
            etPickupDate.setText(pd)
            hasPrefill = true
        }
        if (hasPrefill) {
            pickupDetailsContainer.visibility = View.VISIBLE
            // ensure date field visible (XML had it gone)
            etPickupDate.visibility = View.VISIBLE
        }

        // Initially hide or show the container depending on radio selection
        when (radioGroup.checkedRadioButtonId) {
            R.id.rbDropOffAtFacility -> {
                pickupDetailsContainer.visibility = View.GONE
                etPickupDate.visibility = View.GONE
            }
            R.id.rbPickupFromLocation -> {
                pickupDetailsContainer.visibility = View.VISIBLE
                etPickupDate.visibility = View.VISIBLE
            }
            else -> {
                pickupDetailsContainer.visibility = View.GONE
                etPickupDate.visibility = View.GONE
            }
        }

        setNextEnabled(false)

        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPickupFromLocation -> {
                    Log.d(TAG, "Selected: Pickup from location")
                    Toast.makeText(requireContext(), "Pickup from location selected", Toast.LENGTH_SHORT).show()
                    activity.getWizardData().pickupService = PickupService.PICKUP_FROM_LOCATION
                    pickupDetailsContainer.visibility = View.VISIBLE
                    etPickupDate.visibility = View.VISIBLE
                    validateForm()
                }
                R.id.rbDropOffAtFacility -> {
                    Log.d(TAG, "Selected: Drop off at facility")
                    Toast.makeText(requireContext(), "Drop off at facility selected", Toast.LENGTH_SHORT).show()
                    activity.getWizardData().pickupService = PickupService.DROP_OFF_AT_FACILITY
                    pickupDetailsContainer.visibility = View.GONE
                    etPickupLocation.text.clear()
                    etPickupDate.text.clear()
                    etPickupDate.visibility = View.GONE
                    validateForm()
                }
                else -> {
                    Log.d(TAG, "No pickup option selected")
                    setNextEnabled(false)
                }
            }
        }

        // Re-validate when user types location or date
        etPickupLocation.addTextChangedListener { validateForm() }
        etPickupDate.addTextChangedListener { validateForm() }

        // Ensure initial state matches any pre-selected radio button in XML
        validateForm()

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
            
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && pickupDate.isEmpty()) {
                etPickupDate.error = "Please select a pickup date"
                return@setOnClickListener
            }
            
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation) {
                activity.getWizardData().pickupLocation = location
            }
            
            // Save pickup date only if set (pickup-from-location)
            if (pickupDate.isNotEmpty()) {
                activity.getWizardData().pickupDate = pickupDate
            } else {
                activity.getWizardData().pickupDate = null
            }

            activity.goToNextStep()
        }
    }
    
    private fun validateForm() {
        // If user chose pickup-from-location, require both location and date
        if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation) {
            val enabled = etPickupLocation.text.toString().trim().isNotEmpty() &&
                    etPickupDate.text.toString().trim().isNotEmpty()
            setNextEnabled(enabled)
        } else if (radioGroup.checkedRadioButtonId == R.id.rbDropOffAtFacility) {
            // For drop-off option, no extra inputs required – enable Next
            setNextEnabled(true)
        } else {
            // No selection yet
            setNextEnabled(false)
        }
    }

    private fun setNextEnabled(enabled: Boolean) {
        btnNext.isEnabled = enabled
        btnNext.isClickable = enabled
        // visually indicate disabled state
        btnNext.alpha = if (enabled) 1.0f else 0.5f
        Log.d(TAG, "Next button enabled: $enabled")
    }
}
