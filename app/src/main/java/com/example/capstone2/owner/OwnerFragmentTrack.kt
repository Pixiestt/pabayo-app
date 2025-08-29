package com.example.capstone2.owner

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.RequestAdapter
import com.example.capstone2.adapter.TrackAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.OwnerRequestViewModel
import com.example.capstone2.viewmodel.OwnerRequestViewModelFactory

class OwnerFragmentTrack : Fragment(R.layout.owner_fragment_track) {

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: RequestAdapter
    private lateinit var ownerRequestViewModel: OwnerRequestViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerTrack)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        requestAdapter = RequestAdapter(emptyList()) { /* No action needed for accepted list */ }
        recyclerView.adapter = requestAdapter

        // Get token
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = OwnerRequestViewModelFactory(repository, requireActivity().application)
        ownerRequestViewModel = ViewModelProvider(this, viewModelFactory)[OwnerRequestViewModel::class.java]
        trackAdapter = TrackAdapter(emptyList()) { request, newStatusID ->
            // Call a function to update status
            updateRequestStatus(request.requestID, newStatusID)
        }

        recyclerView.adapter = trackAdapter

        ownerRequestViewModel.ownerRequests.observe(viewLifecycleOwner) { requests ->
            val filteredRequests = requests?.filterNot { it.statusID.toInt() in listOf(1, 8, 9) }
            trackAdapter.updateRequests(filteredRequests.orEmpty())


            if (filteredRequests.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No requests found matching the filter", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch requests
        ownerRequestViewModel.fetchOwnerRequests()
    }

    private fun updateRequestStatus(requestID: Long, newStatusID: Int) {
        ownerRequestViewModel.updateStatus(requestID, newStatusID,
            onSuccess = {
                Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show()
                ownerRequestViewModel.fetchOwnerRequests() // 🔁 Refresh from backend
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        )
    }
}
