package com.example.capstone2.customer

import android.app.DatePickerDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.RequestViewModel
import com.example.capstone2.viewmodel.RequestViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale


class CustomerFragmentRequest : Fragment(R.layout.customer_fragment_request) {


    private lateinit var btnDatePicker: Button
    private lateinit var tvQuantity: TextView
    private lateinit var cbPickup: CheckBox
    private lateinit var cbDelivery: CheckBox
    private lateinit var cbFeedsCon: CheckBox
    private lateinit var etPickupLocation: EditText
    private lateinit var etDeliveryLocation: EditText

    private var quantity = 1
    private val calendarView = Calendar.getInstance()
    private val defaultdatetext = "Pick a schedule"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val btnAdd = view.findViewById<Button>(R.id.btnAdd)
        val btnMinus = view.findViewById<Button>(R.id.btnMinus)
        tvQuantity = view.findViewById(R.id.tvQuantity)
        cbPickup = view.findViewById(R.id.cbPickup)
        cbDelivery = view.findViewById(R.id.cbDelivery)
        cbFeedsCon = view.findViewById(R.id.cbFeedsCon)
        etPickupLocation = view.findViewById(R.id.etPickupLocation)
        etDeliveryLocation = view.findViewById(R.id.etDeliveryLocation)
        val rgPayment = view.findViewById<RadioGroup>(R.id.rgPayment)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        btnDatePicker = view.findViewById(R.id.btnDatePicker)
        val comment = view.findViewById<EditText>(R.id.commentEditText)

        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        val authedApiService = ApiClient.getApiService{token}
        val requestRepository = RequestRepository(authedApiService)
        val factory = RequestViewModelFactory(requestRepository, requireActivity().application)
        val requestViewModel = ViewModelProvider(this, factory)[RequestViewModel::class.java]

        // Initially setup UI elements
        btnDatePicker.visibility = View.VISIBLE // Always show the schedule picker
        etPickupLocation.visibility = View.GONE
        etDeliveryLocation.visibility = View.GONE

        // Handle checkbox state changes
        cbPickup.setOnCheckedChangeListener { _, isChecked ->
            etPickupLocation.visibility = if (isChecked) View.VISIBLE else View.GONE
            // Don't update schedule button visibility - always keep it visible
        }

        cbDelivery.setOnCheckedChangeListener { _, isChecked ->
            etDeliveryLocation.visibility = if (isChecked) View.VISIBLE else View.GONE
            // Don't update schedule button visibility - always keep it visible
        }

        tvQuantity.text = quantity.toString()
        btnAdd.setOnClickListener {
            quantity++
            tvQuantity.text = quantity.toString()
        }
        btnMinus.setOnClickListener {
            if (quantity > 1) {
                quantity--
                tvQuantity.text = quantity.toString()
            }
        }


        val dateBox = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            calendarView.set(Calendar.YEAR, year)
            calendarView.set(Calendar.MONTH, month)
            calendarView.set(Calendar.DAY_OF_MONTH, day)
            updateBtnText(calendarView)
        }
        btnDatePicker.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                dateBox,
                calendarView.get(Calendar.YEAR),
                calendarView.get(Calendar.MONTH),
                calendarView.get(Calendar.DAY_OF_MONTH))
                .show()
        }


        requestViewModel.submitResult.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                Toast.makeText(requireContext(), "Request submitted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Request submission failed", Toast.LENGTH_SHORT).show()
            }
        }

        btnSubmit.setOnClickListener {
            val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            val customerID = sharedPreferences.getLong("userID", -1L)

            if (customerID == -1L) {
                Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pickupChecked = cbPickup.isChecked
            val deliveryChecked = cbDelivery.isChecked
            val feedsConChecked = cbFeedsCon.isChecked // Reserved for future use?

            val selectedDate = btnDatePicker.text.toString()

            // Get location values
            val pickupLocation = if (pickupChecked) etPickupLocation.text.toString() else null
            val deliveryLocation = if (deliveryChecked) etDeliveryLocation.text.toString() else null

            // Validate pickup location when pickup is checked
            if (pickupChecked && pickupLocation.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter pickup location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate delivery location when delivery is checked
            if (deliveryChecked && deliveryLocation.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Please enter delivery location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Always require schedule selection, regardless of service type
            if (selectedDate == defaultdatetext) {
                Toast.makeText(requireContext(), "Please select a schedule date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calculate serviceID
            val serviceID = when {
                pickupChecked && deliveryChecked && feedsConChecked -> 1L
                pickupChecked && deliveryChecked -> 5L
                pickupChecked && feedsConChecked -> 6L
                deliveryChecked && feedsConChecked ->7
                pickupChecked -> 2L
                deliveryChecked -> 3L
                feedsConChecked -> 4L

                else -> 8L
            }



            val selectedPaymentId = rgPayment.checkedRadioButtonId
            val paymentMethod = view.findViewById<RadioButton>(selectedPaymentId)?.text?.toString()

            val modeID = when (paymentMethod?.lowercase()) {
                "cash on delivery" -> 1L
                "gcash" -> 2L
                else -> {
                    Toast.makeText(requireContext(), "Please select a valid payment method", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val request = CreateRequest(
                ownerID = 2L,
                customerID = customerID,
                serviceID = serviceID,
                statusID = 1L,
                courierID = 1L, // temporary
                pickupDate = selectedDate, // Always use the selected date
                deliveryDate = null,
                sackQuantity = quantity,
                comment = comment.text.toString(),
                modeID = modeID,
                pickupLocation = pickupLocation,
                deliveryLocation = deliveryLocation
            )

            requestViewModel.submitRequest(request)
        }


    }

    // Remove the updateScheduleButtonVisibility function since we're not using it anymore
    // The schedule button is always visible

    private fun updateBtnText(calendar: Calendar){
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.UK)
        btnDatePicker.setText(sdf.format(calendar.time))
    }
    
}