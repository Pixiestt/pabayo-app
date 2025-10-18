package com.example.capstone2.customer

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
import com.example.capstone2.data.models.QueueResponse
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.CustomerRequestViewModel
import com.example.capstone2.viewmodel.CustomerRequestViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.network.ApiClient as AuthApiClient
import com.example.capstone2.network.getTokenProvider

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
        // Use SharedPrefManager to handle legacy and canonical prefs
        customerID = SharedPrefManager.getUserId(requireContext()) ?: -1L

        Log.d("CustomerFragmentHome", "Retrieved customerID: $customerID")
        
        // Initialize ViewModel with AUTHORIZED ApiService so requests include Authorization header
        val apiService = AuthApiClient.getApiService(getTokenProvider(requireContext()))
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
            Log.d("CustomerFragmentHome", "Fetching queue + ALL + customer requests for queue view")
            // Preferred: dedicated queue endpoint
            if (customerID > 0) {
                customerRequestViewModel.fetchCustomerQueue(customerID)
            }
            // Fallbacks
            customerRequestViewModel.fetchAllRequests()
            if (customerID > 0) {
                customerRequestViewModel.fetchCustomerRequests(customerID)
            }
            showQueueDialog()
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
        val processingHeader = dialogView.findViewById<TextView>(R.id.processingHeaderText)
        val pendingHeader = dialogView.findViewById<TextView>(R.id.pendingHeaderText)
        val scopeNote = dialogView.findViewById<TextView>(R.id.queueScopeNote)

        // Ensure we don't stack multiple observers if user opens dialog repeatedly
        customerRequestViewModel.allRequests.removeObservers(viewLifecycleOwner)
        customerRequestViewModel.customerRequests.removeObservers(viewLifecycleOwner)
        customerRequestViewModel.queueData.removeObservers(viewLifecycleOwner)

        // Hold latest snapshots
        var latestAll: List<Request>? = null
        var latestCustomer: List<Request>? = null
        var latestQueue: QueueResponse? = null

        fun renderQueue(queue: QueueResponse, source: String) {
            processingContainer.removeAllViews()
            pendingContainer.removeAllViews()

            val processingRequests = queue.processing
            val pendingRequests = queue.pending

            processingHeader.text = getString(R.string.rb_processing) + "  " + getString(R.string.total_processing_count, processingRequests.size)
            pendingHeader.text = getString(R.string.rb_pending) + "  " + getString(R.string.total_pending_count, pendingRequests.size)

            if (processingRequests.isNotEmpty()) {
                processingRequests.forEach { addRequestItemToContainer(it, processingContainer) }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = getString(R.string.no_processing_requests)
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                processingContainer.addView(emptyText)
            }

            if (pendingRequests.isNotEmpty()) {
                pendingRequests.forEach { addRequestItemToContainer(it, pendingContainer) }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = getString(R.string.no_pending_requests)
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                pendingContainer.addView(emptyText)
            }

            scopeNote.visibility = if (source == "customer") View.VISIBLE else View.GONE
        }

        fun renderFromFlatList(requests: List<Request>, source: String) {
            processingContainer.removeAllViews()
            pendingContainer.removeAllViews()

            // Use real backend status IDs
            val PENDING = setOf(1) // Pending Approval
            val PROCESSING = setOf(10, 2, 4, 6, 12) // Accepted, Pickup In Progress, Pickup Completed, Out for Delivery, Milling Done

            val processingRequests = requests.filter { it.statusID in PROCESSING }
            val pendingRequests = requests.filter { it.statusID in PENDING }

            processingHeader.text = getString(R.string.rb_processing) + "  " + getString(R.string.total_processing_count, processingRequests.size)
            pendingHeader.text = getString(R.string.rb_pending) + "  " + getString(R.string.total_pending_count, pendingRequests.size)

            if (processingRequests.isNotEmpty()) {
                processingRequests.forEach { addRequestItemToContainer(it, processingContainer) }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = getString(R.string.no_processing_requests)
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                processingContainer.addView(emptyText)
            }
            if (pendingRequests.isNotEmpty()) {
                pendingRequests.forEach { addRequestItemToContainer(it, pendingContainer) }
            } else {
                val emptyText = TextView(requireContext())
                emptyText.text = getString(R.string.no_pending_requests)
                emptyText.textAlignment = View.TEXT_ALIGNMENT_CENTER
                emptyText.setPadding(8, 8, 8, 8)
                pendingContainer.addView(emptyText)
            }

            scopeNote.visibility = if (source == "customer") View.VISIBLE else View.GONE
        }

        fun chooseAndRender() {
            when {
                // Prefer backend-provided queue grouping
                latestQueue != null && ((latestQueue?.pending?.isNotEmpty() == true) || (latestQueue?.processing?.isNotEmpty() == true)) -> {
                    Log.d("CustomerQueue", "Using QUEUE data for display")
                    renderQueue(latestQueue!!, "customer")
                }
                // Fallback to customer flat list (normalized elsewhere if needed)
                !latestCustomer.isNullOrEmpty() -> {
                    Log.d("CustomerQueue", "Queue empty; using CUSTOMER flat list")
                    renderFromFlatList(latestCustomer!!, "customer")
                }
                // Last resort: ALL
                !latestAll.isNullOrEmpty() -> {
                    Log.d("CustomerQueue", "No customer data; falling back to ALL requests")
                    renderFromFlatList(latestAll!!, "all")
                }
                else -> {
                    // No data: show empties
                    renderQueue(QueueResponse(), "customer")
                }
            }
        }

        // Observe QUEUE
        customerRequestViewModel.queueData.observe(viewLifecycleOwner) { queue ->
            latestQueue = queue
            chooseAndRender()
        }
        // Observe ALL requests
        customerRequestViewModel.allRequests.observe(viewLifecycleOwner) { requests ->
            latestAll = requests
            chooseAndRender()
        }
        // Observe customer-only requests (fallback)
        customerRequestViewModel.customerRequests.observe(viewLifecycleOwner) { requests ->
            latestCustomer = requests
            chooseAndRender()
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
        // Per requirement: show only the Request ID
        requestView.text = getString(R.string.request_number, request.requestID.toString())
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