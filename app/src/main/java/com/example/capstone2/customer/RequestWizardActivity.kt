package com.example.capstone2.customer

import android.content.Context
import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.models.RequestWizardData
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.PickupService
import com.example.capstone2.data.models.DeliveryService
import com.example.capstone2.data.models.Request
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.RequestViewModel
import com.example.capstone2.viewmodel.RequestViewModelFactory

class RequestWizardActivity : AppCompatActivity() {

    private lateinit var requestWizardData: RequestWizardData
    private lateinit var requestViewModel: RequestViewModel
    private var currentStep = 1
    private val totalSteps = 4
    private var editingRequestId: Long? = null

    companion object {
        const val EXTRA_EDIT_REQUEST = "edit_request"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_wizard)

        // Block users whose account status is pending
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val status = sharedPreferences.getString("user_status", "approved")?.trim()?.lowercase()
        if (status == "pending") {
            showPendingDialog()
            return
        }

        requestWizardData = RequestWizardData()
        setupViewModel()

        // Check for edit request passed via intent
        val maybeEdit = intent.getParcelableExtra<Request>(EXTRA_EDIT_REQUEST)
        if (maybeEdit != null) {
            editingRequestId = maybeEdit.requestID
            prefillFromRequest(maybeEdit)
        }

        showStep(1)
    }
    
    private fun showPendingDialog() {
        AlertDialog.Builder(this)
            .setTitle("Account not approved")
            .setMessage("Your account is still pending approval. You cannot avail services until your account is approved.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun setupViewModel() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Missing auth token", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val authedApiService = ApiClient.getApiService { token }
        val requestRepository = RequestRepository(authedApiService)
        val factory = RequestViewModelFactory(requestRepository, application)
        requestViewModel = ViewModelProvider(this, factory)[RequestViewModel::class.java]
        
        requestViewModel.submitResult.observe(this) { response ->
            if (response != null) {
                Toast.makeText(this, "Request submitted successfully", Toast.LENGTH_SHORT).show()
                // inform caller that a request was created (no id available here)
                setResult(Activity.RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Request submission failed", Toast.LENGTH_SHORT).show()
            }
        }

        requestViewModel.updateResult.observe(this) { response ->
            if (response != null) {
                Toast.makeText(this, "Request updated successfully", Toast.LENGTH_SHORT).show()
                // notify other parts of the app that a request was updated so lists can refresh
                val intent = android.content.Intent("com.example.capstone2.ACTION_REQUEST_UPDATED")
                // include request id if available
                intent.putExtra("requestID", editingRequestId)
                sendBroadcast(intent)
                // also set result so the caller (fragment) knows an update happened
                val resultIntent = android.content.Intent()
                resultIntent.putExtra("requestID", editingRequestId)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Failed to update request", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    fun showStep(step: Int) {
        currentStep = step
        val fragment = when (step) {
            1 -> MillingDetailsFragment()
            2 -> PickupServiceFragment()
            3 -> DeliveryServiceFragment()
            4 -> FeedsConversionFragment()
            else -> throw IllegalArgumentException("Invalid step: $step")
        }
        
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
    
    fun goToNextStep() {
        if (currentStep < totalSteps) {
            showStep(currentStep + 1)
        }
    }
    
    fun goToPreviousStep() {
        if (currentStep > 1) {
            showStep(currentStep - 1)
        }
    }
    
    fun submitRequest() {
        val sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val customerID = sharedPreferences.getLong("userID", -1L)
        
        if (customerID == -1L) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Calculate serviceID based on selected services
        val serviceID = calculateServiceID()
        
        // Set default payment method to Cash on Delivery
        val modeID = 1L
        
        val request = CreateRequest(
            ownerID = 2L,
            customerID = customerID,
            serviceID = serviceID,
            statusID = 1L,
            courierID = 1L,
            pickupDate = requestWizardData.pickupDate ?: "",
            deliveryDate = null,
            sackQuantity = requestWizardData.sackCount,
            comment = requestWizardData.comment ?: "",
            modeID = modeID,
            pickupLocation = requestWizardData.pickupLocation,
            deliveryLocation = requestWizardData.deliveryLocation
        )

        // If editing, call update; otherwise create
        if (editingRequestId != null) {
            requestViewModel.updateRequest(editingRequestId!!, request)
        } else {
            requestViewModel.submitRequest(request)
        }
    }
    
    private fun calculateServiceID(): Long {
        val pickupChecked = requestWizardData.pickupService == PickupService.PICKUP_FROM_LOCATION
        val deliveryChecked = requestWizardData.deliveryService == DeliveryService.DELIVER_TO_LOCATION
        val feedsConChecked = requestWizardData.feedsConversion == true
        
        return when {
            pickupChecked && deliveryChecked && feedsConChecked -> 1L
            pickupChecked && deliveryChecked -> 5L
            pickupChecked && feedsConChecked -> 6L
            deliveryChecked && feedsConChecked -> 7L
            pickupChecked -> 2L
            deliveryChecked -> 3L
            feedsConChecked -> 4L
            else -> 8L
        }
    }
    
    fun getWizardData(): RequestWizardData = requestWizardData
    fun getCurrentStep(): Int = currentStep

    private fun prefillFromRequest(req: Request) {
        // Map fields from Request to RequestWizardData so fragments pick them up
        requestWizardData.sackCount = req.sackQuantity
        requestWizardData.comment = req.comment
        requestWizardData.pickupDate = if (!req.pickupDate.isNullOrEmpty()) req.pickupDate else null
        requestWizardData.pickupLocation = req.pickupLocation
        requestWizardData.deliveryLocation = req.deliveryLocation

        when (req.serviceID) {
            1L -> {
                requestWizardData.pickupService = PickupService.PICKUP_FROM_LOCATION
                requestWizardData.deliveryService = DeliveryService.DELIVER_TO_LOCATION
                requestWizardData.feedsConversion = true
            }
            2L -> {
                requestWizardData.pickupService = PickupService.PICKUP_FROM_LOCATION
                requestWizardData.deliveryService = null
                requestWizardData.feedsConversion = false
            }
            3L -> {
                requestWizardData.pickupService = null
                requestWizardData.deliveryService = DeliveryService.DELIVER_TO_LOCATION
                requestWizardData.feedsConversion = false
            }
            4L -> {
                requestWizardData.pickupService = null
                requestWizardData.deliveryService = null
                requestWizardData.feedsConversion = true
            }
            5L -> {
                requestWizardData.pickupService = PickupService.PICKUP_FROM_LOCATION
                requestWizardData.deliveryService = DeliveryService.DELIVER_TO_LOCATION
                requestWizardData.feedsConversion = false
            }
            6L -> {
                requestWizardData.pickupService = PickupService.PICKUP_FROM_LOCATION
                requestWizardData.deliveryService = null
                requestWizardData.feedsConversion = true
            }
            7L -> {
                requestWizardData.pickupService = null
                requestWizardData.deliveryService = DeliveryService.DELIVER_TO_LOCATION
                requestWizardData.feedsConversion = true
            }
            else -> {
                // leave defaults
            }
        }
    }
}
