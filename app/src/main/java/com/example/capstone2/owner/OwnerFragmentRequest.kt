package com.example.capstone2.owner

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.capstone2.R
import com.example.capstone2.adapter.RequestAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.OwnerRequestViewModel
import com.example.capstone2.viewmodel.OwnerRequestViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OwnerFragmentRequest : Fragment(R.layout.owner_fragment_request) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: RequestAdapter
    private lateinit var ownerRequestViewModel: OwnerRequestViewModel
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerRequests)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Initialize adapter with button click handler
        requestAdapter = RequestAdapter(emptyList()) { request ->
            showActionDialog(request)
        }

        recyclerView.adapter = requestAdapter

        // Retrieve token
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Set up ViewModel with repository
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = OwnerRequestViewModelFactory(repository, requireActivity().application)
        ownerRequestViewModel = ViewModelProvider(this, viewModelFactory)[OwnerRequestViewModel::class.java]

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshRequestsList()
        }
        
        // Observe LiveData from ViewModel
        ownerRequestViewModel.ownerRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("OwnerFragmentRequest", "Received ${requests?.size ?: 0} requests from ViewModel")
            
            // ONLY show requests with statusID 1 (Subject for approval)
            val pendingRequests = requests?.filter { 
                try {
                    it.statusID.toInt() == 1 // Only show "Subject for approval" requests
                } catch (e: NumberFormatException) {
                    Log.e("OwnerFragmentRequest", "Invalid status ID: ${it.statusID}", e)
                    false
                }
            }
            
            Log.d("OwnerFragmentRequest", "Filtered to ${pendingRequests?.size ?: 0} pending requests")
            
            // Stop the refresh animation if it's running
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
            
            if (!pendingRequests.isNullOrEmpty()) {
                requestAdapter.updateRequests(pendingRequests)
            } else {
                requestAdapter.updateRequests(emptyList())
                Toast.makeText(requireContext(), "No pending approval requests found", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch owner requests from API
        refreshRequestsList()
    }
    
    private fun showActionDialog(request: com.example.capstone2.data.models.Request) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Request Action")
            .setMessage("Do you want to accept or reject this request?")
            .setPositiveButton("Accept") { _, _ ->
                // Show loading state
                swipeRefreshLayout.isRefreshing = true
                
                ownerRequestViewModel.acceptRequest(
                    request,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Accepted ${request.customerName}", Toast.LENGTH_SHORT).show()
                        // Refresh the list after accepting
                        refreshRequestsList()
                    },
                    onError = { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        // Stop the loading state
                        swipeRefreshLayout.isRefreshing = false
                    }
                )
            }
            .setNegativeButton("Reject") { _, _ ->
                // Show loading state
                swipeRefreshLayout.isRefreshing = true
                
                ownerRequestViewModel.rejectRequest(
                    request,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Rejected ${request.customerName}", Toast.LENGTH_SHORT).show()
                        // Refresh the list after rejecting
                        refreshRequestsList()
                    },
                    onError = { msg ->
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        // Stop the loading state
                        swipeRefreshLayout.isRefreshing = false
                    }
                )
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    private fun refreshRequestsList() {
        CoroutineScope(Dispatchers.Main).launch {
            // Start the refresh animation if it's not already running
            if (!swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = true
            }
            
            delay(300) // Small delay to ensure server has processed the update
            ownerRequestViewModel.fetchOwnerRequests()
            Log.d("OwnerFragmentRequest", "Refreshing request list")
        }
    }
}

