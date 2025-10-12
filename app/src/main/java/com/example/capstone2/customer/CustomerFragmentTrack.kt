package com.example.capstone2.customer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.CustomerTrackAdapter
import com.example.capstone2.data.models.Request
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.CustomerRequestViewModel
import com.example.capstone2.viewmodel.CustomerRequestViewModelFactory

class CustomerFragmentTrack : Fragment(R.layout.customer_fragment_track) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var customerTrackAdapter: CustomerTrackAdapter
    private lateinit var customerRequestViewModel: CustomerRequestViewModel
    private lateinit var tvNoRequests: TextView
    private var customerID: Long = -1L

    // Activity result launcher for editing requests
    private lateinit var editLauncher: ActivityResultLauncher<Intent>

    // BroadcastReceiver to refresh list when a request is updated elsewhere
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Optionally, we could inspect the requestID from the intent
            Log.d("CustomerFragmentTrack", "Received ACTION_REQUEST_UPDATED, refreshing list")
            fetchCustomerRequests()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewCustomerTrack)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)

        // Prepare activity result launcher before using it
        editLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                Log.d("CustomerFragmentTrack", "Edit returned RESULT_OK, refreshing list")
                fetchCustomerRequests()
            } else {
                Log.d("CustomerFragmentTrack", "Edit returned resultCode=${result.resultCode}")
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Initialize adapter with callback for mark complete button
        customerTrackAdapter = CustomerTrackAdapter(emptyList(), { request ->
            handleMarkComplete(request)
        }, { request ->
            // Launch the RequestWizardActivity for editing and wait for result
            val intent = Intent(requireContext(), RequestWizardActivity::class.java)
            intent.putExtra(RequestWizardActivity.EXTRA_EDIT_REQUEST, request)
            editLauncher.launch(intent)
        })
        recyclerView.adapter = customerTrackAdapter

        // Register receiver to listen for updates (so list refreshes after edit)
        val filter = IntentFilter("com.example.capstone2.ACTION_REQUEST_UPDATED")
        // Register with explicit NOT_EXPORTED flag to avoid unprotected broadcast lint
        try {
            requireContext().registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } catch (ex: Exception) {
            // Fallback for very old platforms if needed
            try { requireContext().registerReceiver(updateReceiver, filter) } catch (_: Exception) {}
        }

        // Get token and customer ID from shared preferences
        val token = SharedPrefManager.getAuthToken(requireContext())
        customerID = SharedPrefManager.getUserId(requireContext()) ?: -1L

        // Show customerID in Toast for debugging
        Toast.makeText(requireContext(), "Customer ID: $customerID", Toast.LENGTH_LONG).show()
        Log.d("CustomerTrack", "CustomerID from preferences: $customerID")
        Log.d("CustomerTrack", "Token present=${!token.isNullOrBlank()}")

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        if (customerID == -1L) {
            Toast.makeText(requireContext(), "Customer ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = CustomerRequestViewModelFactory(repository)
        customerRequestViewModel = ViewModelProvider(this, viewModelFactory)[CustomerRequestViewModel::class.java]

        // Observe requests
        customerRequestViewModel.customerRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("CustomerTrack", "Received ${requests?.size ?: 0} requests in fragment")
            
            if (requests != null && requests.isNotEmpty()) {
                customerTrackAdapter.updateRequests(requests)
                recyclerView.visibility = View.VISIBLE
                tvNoRequests.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                tvNoRequests.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "No requests found", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe update status result
        customerRequestViewModel.updateStatusResult.observe(viewLifecycleOwner) { isSuccess ->
            if (isSuccess) {
                Toast.makeText(requireContext(), "Request marked as complete", Toast.LENGTH_SHORT).show()
                // Refresh list after successfully marking as complete
                fetchCustomerRequests()
            } else {
                Toast.makeText(requireContext(), "Failed to update request status", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch customer's requests
        fetchCustomerRequests()
    }
    
    override fun onResume() {
        super.onResume()
        // Ensure we refresh the list every time the fragment becomes visible
        fetchCustomerRequests()
    }

    private fun handleMarkComplete(request: Request) {
        customerRequestViewModel.markRequestAsComplete(request.requestID)
    }
    
    private fun fetchCustomerRequests() {
        if (customerID != -1L) {
            customerRequestViewModel.fetchCustomerRequests(customerID)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(updateReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered or already unregistered, ignore
        }
    }
}