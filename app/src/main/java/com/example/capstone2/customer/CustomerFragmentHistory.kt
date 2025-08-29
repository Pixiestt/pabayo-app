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
import com.example.capstone2.adapter.CustomerHistoryAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.CustomerHistoryViewModel
import com.example.capstone2.viewmodel.CustomerHistoryViewModelFactory

class CustomerFragmentHistory : Fragment(R.layout.customer_fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: CustomerHistoryAdapter
    private lateinit var historyViewModel: CustomerHistoryViewModel
    private lateinit var tvNoHistory: TextView
    private var customerID: Long = -1L

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = CustomerHistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        // Get token and customer ID from shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        customerID = sharedPreferences.getLong("userID", -1L)

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
        val viewModelFactory = CustomerHistoryViewModelFactory(repository)
        historyViewModel = ViewModelProvider(this, viewModelFactory)[CustomerHistoryViewModel::class.java]

        // Observe completed requests
        historyViewModel.completedRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("CustomerHistory", "Received ${requests?.size ?: 0} completed requests in fragment")
            
            if (requests != null && requests.isNotEmpty()) {
                historyAdapter.updateRequests(requests)
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "No completed requests found", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch customer's completed requests
        fetchCompletedRequests()
    }
    
    private fun fetchCompletedRequests() {
        if (customerID != -1L) {
            historyViewModel.fetchCompletedRequests(customerID)
        }
    }
    
    override fun onResume() {
        super.onResume()
        fetchCompletedRequests()
    }
}