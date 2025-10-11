package com.example.capstone2.customer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.api.ApiClient
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.CustomerRequestViewModel
import com.example.capstone2.viewmodel.CustomerRequestViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomerFragmentHome : Fragment(R.layout.customer_fragment_home) {
    
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var customerRequestViewModel: CustomerRequestViewModel
    private var customerID: Long = -1 // Will be set from SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.customer_fragment_home, container, false)

        // Get customer ID from SharedPreferences - using the correct key "userID"
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        customerID = sharedPreferences.getLong("userID", -1)
        
        Log.d("CustomerFragmentHome", "Retrieved customerID: $customerID")
        
        // Initialize ViewModel
        val apiService = ApiClient.apiService
        val repository = RequestRepository(apiService)
        val viewModelFactory = CustomerRequestViewModelFactory(repository)
        customerRequestViewModel = ViewModelProvider(this, viewModelFactory)[CustomerRequestViewModel::class.java]
        
        drawerLayout = requireActivity().findViewById(R.id.drawer_layout)
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView)
        
        // Ensure bottom nav is hidden on home page
        bottomNavigationView.visibility = View.GONE
        
        // Get buttons
        val menuButton = view.findViewById<ImageButton>(R.id.menuButton)
        val servicerateBtn = view.findViewById<Button>(R.id.serviceRateBtn)
        val viewQueueBtn = view.findViewById<Button>(R.id.viewQueueBtn)

        // Set click listeners
        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        
        servicerateBtn.setOnClickListener {
            showServiceRatesDialog()
        }
        
        viewQueueBtn.setOnClickListener {
            // Fetch requests and show queue dialog
            if (customerID > 0) {
                Log.d("CustomerFragmentHome", "Fetching requests for customer ID: $customerID")
                customerRequestViewModel.fetchCustomerRequests(customerID)
                showQueueDialog()
            } else {
                Log.e("CustomerFragmentHome", "Invalid customerID: $customerID")
                AlertDialog.Builder(requireContext())
                    .setTitle("Error")
                    .setMessage("Could not retrieve your account information. Please log in again.")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
        
        return view
    }

    private fun showServiceRatesDialog(){
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.servicerateslayout, null)

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }
    
    private fun showQueueDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.queue_layout, null)
        
        val processingContainer = dialogView.findViewById<LinearLayout>(R.id.processingContainer)
        val pendingContainer = dialogView.findViewById<LinearLayout>(R.id.pendingContainer)
        
        // Observe requests for queue display
        customerRequestViewModel.customerRequests.observe(viewLifecycleOwner) { requests ->
            // Clear previous views
            processingContainer.removeAllViews()
            pendingContainer.removeAllViews()
            
            // Log all requests for debugging
            Log.d("CustomerQueue", "Total requests received: ${requests.size}")
            requests.forEach { req ->
                Log.d("CustomerQueue", "Request ID: ${req.requestID}, Status: ${req.statusID}, Service: ${req.serviceName}")
            }
            
            // Filter and sort requests
            val processingRequests = requests.filter { 
                try {
                    val status = it.statusID.toInt()
                    status == 5 // Status 5 = Processing
                } catch (e: NumberFormatException) {
                    Log.e("CustomerQueue", "Invalid statusID: ${it.statusID}")
                    false
                }
            }
            
            val pendingRequests = requests.filter { 
                try {
                    val status = it.statusID.toInt()
                    status == 2 || // Status 2 = Delivery boy pickup
                    status == 3 || // Status 3 = Waiting for customer drop off
                    status == 4 || // Status 4 = Pending
                    status == 6 || // Status 6 = Rider out for delivery
                    status == 7 || // Status 7 = Waiting for customer pickup
                    status == 10 || // Status 10 = Request Accepted
                    status == 11 ||  // Status 11 = Partially Accepted
                    status == 12 // Status 12 = Milling done
                } catch (e: NumberFormatException) {
                    Log.e("CustomerQueue", "Invalid statusID: ${it.statusID}")
                    false
                }
            }
            
            // Log filtered requests for debugging
            Log.d("CustomerQueue", "Processing requests: ${processingRequests.size}")
            Log.d("CustomerQueue", "Pending requests: ${pendingRequests.size}")
            
            // Add processing requests to left container
            if (processingRequests.isNotEmpty()) {
                for (request in processingRequests) {
                    Log.d("CustomerQueue", "Adding processing request ID: ${request.requestID}")
                    addRequestItemToContainer(request, processingContainer)
                }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = "No processing requests"
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                processingContainer.addView(emptyText)
            }
            
            // Add pending requests to right container
            if (pendingRequests.isNotEmpty()) {
                for (request in pendingRequests) {
                    Log.d("CustomerQueue", "Adding pending request ID: ${request.requestID}")
                    addRequestItemToContainer(request, pendingContainer)
                }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = "No pending requests"
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                pendingContainer.addView(emptyText)
            }
        }
        
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Queue Status")
            .setCancelable(true)
        
        val dialog = dialogBuilder.create()
        dialog.show()
    }
    
    private fun addRequestItemToContainer(request: Request, container: LinearLayout) {
        val requestView = TextView(requireContext())
        // Display more useful information than just the request ID
        requestView.text = "Request #${request.requestID}\nService: ${request.serviceName}\nQuantity: ${request.sackQuantity}"
        requestView.textSize = 16f
        requestView.setPadding(16, 16, 16, 16)
        requestView.setTextColor(android.graphics.Color.BLACK)
        requestView.setBackgroundResource(R.drawable.rounded_corner_background)
        requestView.textAlignment = View.TEXT_ALIGNMENT_CENTER
        
        // Add margins between items
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(8, 8, 8, 8)
        requestView.layoutParams = params
        
        container.addView(requestView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Make sure bottom nav is visible when leaving this fragment
        bottomNavigationView.visibility = View.VISIBLE
    }
}