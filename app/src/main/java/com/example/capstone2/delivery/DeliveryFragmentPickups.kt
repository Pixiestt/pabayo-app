package com.example.capstone2.delivery

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.DeliveryAction
import com.example.capstone2.adapter.DeliveryCardAdapter
import com.example.capstone2.adapter.DeliveryMode
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.Request
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.repository.SharedPrefManager
import kotlinx.coroutines.launch

class DeliveryFragmentPickups : Fragment(R.layout.fragment_delivery_pickups) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var requestRepository: RequestRepository
    private lateinit var tvNoRequests: TextView
    private lateinit var apiService: ApiService
    private var deliveryAdapter: DeliveryCardAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewDeliveryPickups)
        tvNoRequests = view.findViewById(R.id.tvNoRequestsPickups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Initialize ApiService using token
        val token = SharedPrefManager.getAuthToken(requireContext())
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }
        apiService = ApiClient.getApiService { token }
        requestRepository = RequestRepository(apiService)

        // Initialize Adapter - delegate actions to fragment (no direct network inside adapter)
        deliveryAdapter = DeliveryCardAdapter(
            mutableListOf(),
            DeliveryMode.PICKUPS,
            { request: Request, action: DeliveryAction ->
                when (action) {
                    DeliveryAction.INITIATE -> markPickupInitiated(request)
                    DeliveryAction.DONE -> markPickupDone(request)
                }
            },
            requestRepository
        )
        recyclerView.adapter = deliveryAdapter

        // Fetch pickup-eligible requests
        fetchApprovedPickups()
    }

    private fun fetchApprovedPickups() {
        lifecycleScope.launch {
            try {
                val requests = requestRepository.getDeliveryBoyRequests()
                    .filter { request ->
                        // Let backend decide eligibility; only filter by pickup-related statuses
                        // 10=Request Accepted (ready to initiate pickup), 2=Delivery boy pickup ongoing, 11=Partially accepted (optional)
                        request.statusID == 10 || request.statusID == 2 || request.statusID == 11
                    }

                deliveryAdapter?.submit(requests)
                tvNoRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching requests: ${e.message}", Toast.LENGTH_SHORT).show()
                tvNoRequests.visibility = View.VISIBLE
            }
        }
    }

    private fun markPickupInitiated(request: Request) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.updateRequestStatus(request.requestID, 2) // 2 = Delivery boy pickup
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Pickup initiated", Toast.LENGTH_SHORT).show()
                    fetchApprovedPickups()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${response.code()} ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error initiating pickup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markPickupDone(request: Request) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.markPickupDone(request.requestID)
                if (response.isSuccessful) {
                    // Also set status to Pending (4) as per previous behavior
                    try {
                        requestRepository.updateRequestStatus(request.requestID, 4)
                    } catch (_: Exception) { /* ignore */ }
                    Toast.makeText(requireContext(), "Pickup marked done", Toast.LENGTH_SHORT).show()
                    fetchApprovedPickups()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${response.code()} ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error marking pickup done: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
