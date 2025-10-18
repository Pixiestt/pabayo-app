package com.example.capstone2.customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.capstone2.R
import com.example.capstone2.data.models.PickupService
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory

class PickupServiceFragment : Fragment() {
    
    private lateinit var radioGroup: RadioGroup
    private lateinit var pickupDetailsContainer: View
    private lateinit var etPickupLocation: EditText
    private lateinit var etPickupDate: EditText
    private lateinit var etPickupTime: EditText
    private lateinit var btnNext: Button
    private lateinit var tvStepProgress: TextView
    private lateinit var cbUseSignupAddress: CheckBox

    private lateinit var userViewModel: UserViewModel
    private var savedHomeAddress: String = ""

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private val timeFormat24 = SimpleDateFormat("HH:mm", Locale.UK)
    private val TAG = "PickupServiceFragment"

    // Business hour limits (inclusive). Only allow pickup times between these hours (08:00 - 17:00)
    private val BUSINESS_START_HOUR = 8
    private val BUSINESS_END_HOUR = 17

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
        etPickupTime = view.findViewById(R.id.etPickupTime)
        btnNext = view.findViewById(R.id.btnNext)
        tvStepProgress = view.findViewById(R.id.tvStepProgress)
        cbUseSignupAddress = view.findViewById(R.id.cbUseSignupAddress)

        userViewModel = ViewModelProvider(
            requireActivity(),
            UserViewModelFactory(requireContext())
        )[UserViewModel::class.java]

        val activity = requireActivity() as RequestWizardActivity
        tvStepProgress.text = "2/4"

        // Observe user profile to get saved home address
        userViewModel.getProfile().observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // user.homeAddress is non-null in this project model
                savedHomeAddress = user.homeAddress ?: ""

                // If wizard had the same address previously selected, reflect that in the checkbox
                val wizard = activity.getWizardData()
                if (!savedHomeAddress.isBlank() && wizard.pickupLocation == savedHomeAddress) {
                    cbUseSignupAddress.isChecked = true
                    etPickupLocation.setText(savedHomeAddress)
                    etPickupLocation.isEnabled = false
                }

                // If the user already checked the checkbox while profile was loading,
                // apply the saved home address now so the field is populated and locked.
                if (cbUseSignupAddress.isChecked && savedHomeAddress.isNotBlank()) {
                    etPickupLocation.setText(savedHomeAddress)
                    etPickupLocation.isEnabled = false
                }
            }
        }

        // Prefill from wizard data if present
        val wizard = activity.getWizardData()
        wizard.pickupService?.let { ps ->
            when (ps) {
                PickupService.PICKUP_FROM_LOCATION -> radioGroup.check(R.id.rbPickupFromLocation)
                PickupService.DROP_OFF_AT_FACILITY -> radioGroup.check(R.id.rbDropOffAtFacility)
            }
        }

        // If wizard has stored pickup location/date/time, show container and set values
        var hasPrefill = false
        wizard.pickupLocation?.let { loc ->
            etPickupLocation.setText(loc)
            hasPrefill = true
        }
        wizard.pickupDate?.let { pd ->
            // Only prefill if the stored date is today or in the future
            val parsed = try { dateFormat.parse(pd) } catch (_: Exception) { null }
            val isPast = parsed?.time?.let { it < startOfTodayMillis() } ?: true
            if (!isPast) {
                etPickupDate.setText(pd)
                hasPrefill = true
            } else {
                // Clear stale past date from wizard and notify user
                wizard.pickupDate = null
                etPickupDate.setText("")
                Toast.makeText(requireContext(), "Previous pickup date was in the past. Please choose a new date.", Toast.LENGTH_SHORT).show()
            }
        }
        // If a time is prefilled but outside business hours, clear it (don't auto-accept invalid times)
        wizard.pickupTime?.let { pt ->
            if (isTimeWithinBusinessHours(pt)) {
                etPickupTime.setText(pt)
                hasPrefill = true
            } else {
                // drop invalid prefills so user notices and chooses a valid time
                wizard.pickupTime = null
                // don't spam user with toast on fragment load, but set hint for clarity
                etPickupTime.hint = "Select a pickup time (08:00-17:00)"
            }
        }
        if (hasPrefill) {
            pickupDetailsContainer.visibility = View.VISIBLE
            // ensure date/time fields visible (XML had them gone)
            etPickupDate.visibility = View.VISIBLE
            etPickupTime.visibility = View.VISIBLE
        }

        // Initially hide or show the container depending on radio selection
        when (radioGroup.checkedRadioButtonId) {
            R.id.rbDropOffAtFacility -> {
                pickupDetailsContainer.visibility = View.GONE
                etPickupDate.visibility = View.GONE
                etPickupTime.visibility = View.GONE
            }
            R.id.rbPickupFromLocation -> {
                pickupDetailsContainer.visibility = View.VISIBLE
                etPickupDate.visibility = View.VISIBLE
                etPickupTime.visibility = View.VISIBLE
            }
            else -> {
                pickupDetailsContainer.visibility = View.GONE
                etPickupDate.visibility = View.GONE
                etPickupTime.visibility = View.GONE
            }
        }

        setNextEnabled(false)

        // Checkbox listener: autofill with signup address and disable editing when checked
        cbUseSignupAddress.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // If we don't yet have the saved home address, try to read from ViewModel immediately
                if (savedHomeAddress.isBlank()) {
                    // Will be filled by observer when profile loads; clear for now
                    etPickupLocation.setText("")
                } else {
                    etPickupLocation.setText(savedHomeAddress)
                }
                etPickupLocation.isEnabled = false
                etPickupLocation.isFocusable = false
                // Immediately reflect selection in wizard data
                activity.getWizardData().pickupLocation = if (savedHomeAddress.isNotBlank()) savedHomeAddress else activity.getWizardData().pickupLocation
            } else {
                // Allow user to edit a custom address
                // If wizard was using the saved home address, clear it so user can provide a custom one
                val wizardData = activity.getWizardData()
                if (wizardData.pickupLocation == savedHomeAddress) {
                    wizardData.pickupLocation = null
                }
                etPickupLocation.isEnabled = true
                etPickupLocation.isFocusableInTouchMode = true
            }
            validateForm()
        }

        // Set up radio button listeners
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbPickupFromLocation -> {
                    Log.d(TAG, "Selected: Pickup from location")
                    Toast.makeText(requireContext(), "Pickup from location selected", Toast.LENGTH_SHORT).show()
                    activity.getWizardData().pickupService = PickupService.PICKUP_FROM_LOCATION
                    pickupDetailsContainer.visibility = View.VISIBLE
                    etPickupDate.visibility = View.VISIBLE
                    etPickupTime.visibility = View.VISIBLE
                    validateForm()
                }
                R.id.rbDropOffAtFacility -> {
                    Log.d(TAG, "Selected: Drop off at facility")
                    Toast.makeText(requireContext(), "Drop off at facility selected", Toast.LENGTH_SHORT).show()
                    activity.getWizardData().pickupService = PickupService.DROP_OFF_AT_FACILITY
                    pickupDetailsContainer.visibility = View.GONE
                    etPickupLocation.text.clear()
                    etPickupDate.text.clear()
                    etPickupTime.text.clear()
                    etPickupDate.visibility = View.GONE
                    etPickupTime.visibility = View.GONE
                    // reset checkbox when hiding
                    cbUseSignupAddress.isChecked = false
                    // also clear saved wizard data when hiding
                    val wiz = activity.getWizardData()
                    wiz.pickupLocation = null
                    wiz.pickupDate = null
                    wiz.pickupTime = null
                    validateForm()
                }
                else -> {
                    Log.d(TAG, "No pickup option selected")
                    setNextEnabled(false)
                }
            }
        }

        // Re-validate when user types location or date/time
        etPickupLocation.addTextChangedListener { validateForm() }
        etPickupDate.addTextChangedListener { validateForm() }
        etPickupTime.addTextChangedListener { /* time optional, not gating next */ }

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
            val dialog = DatePickerDialog(
                requireContext(),
                datePickerListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            // Disallow selecting past dates
            dialog.datePicker.minDate = startOfTodayMillis()
            dialog.show()
        }

        // Set up time picker (optional)
        etPickupTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, selHour, selMinute ->
                // Store in 24h HH:mm format to match backend normalization
                val cal = (calendar.clone() as Calendar)
                cal.set(Calendar.HOUR_OF_DAY, selHour)
                cal.set(Calendar.MINUTE, selMinute)
                val selected = timeFormat24.format(cal.time)
                if (!isTimeWithinBusinessHours(selected)) {
                    Toast.makeText(requireContext(), "Pickup time must be between 08:00 and 17:00", Toast.LENGTH_SHORT).show()
                    // do not set invalid time
                } else {
                    etPickupTime.setText(selected)
                }
            }, hour, minute, true).show()
        }

        btnNext.setOnClickListener {
            val location = etPickupLocation.text.toString().trim()
            val pickupDate = etPickupDate.text.toString().trim()
            val pickupTime = etPickupTime.text.toString().trim().ifEmpty { null }

            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && location.isEmpty()) {
                etPickupLocation.error = "Please enter your location"
                return@setOnClickListener
            }
            
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && pickupDate.isEmpty()) {
                etPickupDate.error = "Please select a pickup date"
                return@setOnClickListener
            }
            
            // Extra guard: block past dates if somehow present
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && pickupDate.isNotEmpty()) {
                val parsed = try { dateFormat.parse(pickupDate) } catch (_: Exception) { null }
                val isPast = parsed?.time?.let { it < startOfTodayMillis() } ?: true
                if (isPast) {
                    etPickupDate.error = "Pickup date can’t be in the past"
                    Toast.makeText(requireContext(), "Please choose today or a future date.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Validate pickup time if provided: only allow 08:00 - 17:00 for pickup service
            if (radioGroup.checkedRadioButtonId == R.id.rbPickupFromLocation && pickupTime != null) {
                if (!isTimeWithinBusinessHours(pickupTime)) {
                    etPickupTime.error = "Pickup time must be between 08:00 and 17:00"
                    Toast.makeText(requireContext(), "Pickup time must be between 08:00 and 17:00", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
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
            // Save pickup time (optional)
            activity.getWizardData().pickupTime = pickupTime

            activity.goToNextStep()
        }
    }
    
    private fun validateForm() {
        // If user chose pickup-from-location, require both location and date; time is optional
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

    private fun startOfTodayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    // Helper: validate a time string in HH:mm is within business hours (inclusive)
    private fun isTimeWithinBusinessHours(timeStr: String?): Boolean {
        if (timeStr.isNullOrBlank()) return false
        return try {
            val parsed = timeFormat24.parse(timeStr)
            val cal = Calendar.getInstance()
            cal.time = parsed
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            // allow any minute within the hour range; inclusive of start and end hours
            if (hour < BUSINESS_START_HOUR) return false
            if (hour > BUSINESS_END_HOUR) return false
            // When hour == BUSINESS_END_HOUR (17), allow minutes == 0 only to enforce up-to-17:00 exactly
            if (hour == BUSINESS_END_HOUR && minute > 0) return false
            true
        } catch (e: Exception) {
            false
        }
    }
}
