package com.example.capstone2.owner

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
import com.example.capstone2.adapter.OwnerHistoryAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.OwnerHistoryViewModel
import com.example.capstone2.viewmodel.OwnerHistoryViewModelFactory

class OwnerFragmentHistory : Fragment(R.layout.owner_fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: OwnerHistoryAdapter
    private lateinit var historyViewModel: OwnerHistoryViewModel
    private lateinit var tvNoHistory: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerHistory)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = OwnerHistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        // Get token from shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = OwnerHistoryViewModelFactory(repository)
        historyViewModel = ViewModelProvider(this, viewModelFactory)[OwnerHistoryViewModel::class.java]

        // Observe completed requests
        historyViewModel.completedRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("OwnerHistory", "Received ${requests.size} completed requests in fragment")
            
            if (requests.isNotEmpty()) {
                historyAdapter.updateRequests(requests)
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "No completed requests found", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch owner's completed requests
        fetchCompletedRequests()
    }
    
    private fun fetchCompletedRequests() {
        historyViewModel.fetchCompletedRequests()
    }
    
    override fun onResume() {
        super.onResume()
        fetchCompletedRequests()
    }
}