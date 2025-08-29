package com.example.capstone2.customer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.CustomerTrackAdapter
import com.example.capstone2.data.models.Request
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.CustomerRequestViewModel
import com.example.capstone2.viewmodel.CustomerRequestViewModelFactory

class CustomerFragmentTrack : Fragment(R.layout.customer_fragment_track) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var customerTrackAdapter: CustomerTrackAdapter
    private lateinit var customerRequestViewModel: CustomerRequestViewModel
    private lateinit var tvNoRequests: TextView
    private var customerID: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewCustomerTrack)
        tvNoRequests = view.findViewById(R.id.tvNoRequests)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        
        // Initialize adapter with callback for mark complete button
        customerTrackAdapter = CustomerTrackAdapter(emptyList()) { request ->
            handleMarkComplete(request)
        }
        recyclerView.adapter = customerTrackAdapter

        // Get token and customer ID from shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        customerID = sharedPreferences.getLong("userID", -1L)

        // Show customerID in Toast for debugging
        Toast.makeText(requireContext(), "Customer ID: $customerID", Toast.LENGTH_LONG).show()
        Log.d("CustomerTrack", "CustomerID from preferences: $customerID")
        Log.d("CustomerTrack", "Token: ${token?.take(10)}...")

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
            } else {
                Toast.makeText(requireContext(), "Failed to update request status", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch customer's requests
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
}