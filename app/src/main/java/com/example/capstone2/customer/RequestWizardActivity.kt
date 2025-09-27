package com.example.capstone2.customer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.models.RequestWizardData
import com.example.capstone2.data.models.CreateRequest
import com.example.capstone2.data.models.PickupService
import com.example.capstone2.data.models.DeliveryService
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.RequestViewModel
import com.example.capstone2.viewmodel.RequestViewModelFactory

class RequestWizardActivity : AppCompatActivity() {

    private lateinit var requestWizardData: RequestWizardData
    private lateinit var requestViewModel: RequestViewModel
    private var currentStep = 1
    private val totalSteps = 5
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_wizard)
        
        requestWizardData = RequestWizardData()
        setupViewModel()
        showStep(1)
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
                finish()
            } else {
                Toast.makeText(this, "Request submission failed", Toast.LENGTH_SHORT).show()
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
            5 -> ContactDetailsFragment()
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
        
        requestViewModel.submitRequest(request)
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
    fun getTotalSteps(): Int = totalSteps
}
