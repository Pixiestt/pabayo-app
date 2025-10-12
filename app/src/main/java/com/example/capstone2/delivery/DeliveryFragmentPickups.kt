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

    private lateinit var repo: RequestRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewDeliveryPickups)
        tvNoRequests = view.findViewById(R.id.tvNoRequestsPickups)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ✅ Initialize ApiService first
        apiService = ApiClient.apiService // <-- replace with your Retrofit builder

        // 2️⃣ Initialize repository
        requestRepository = RequestRepository(apiService)

        // Get token
        val token = SharedPrefManager.getAuthToken(requireContext())
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Create authenticated ApiService + Repository
        val authedApiService = ApiClient.getApiService { token ?: "" }
        requestRepository = RequestRepository(authedApiService)

        // Initialize Adapter
        deliveryAdapter = DeliveryCardAdapter(
            mutableListOf(),
            DeliveryMode.PICKUPS,
            { request: Request, action: DeliveryAction ->  // Explicit types help Kotlin infer correctly
                when (action) {
                    DeliveryAction.INITIATE -> {
                        // Optionally update UI here
                    }
                    DeliveryAction.DONE -> {
                        markPickupDone(request)
                    }
                }
            },
            requestRepository  // <- pass the repository here
        )
        recyclerView.adapter = deliveryAdapter

        // Fetch approved pickups (statusID = 2)
        fetchApprovedPickups()
    }

    private fun fetchApprovedPickups() {
        lifecycleScope.launch {
            try {
                val requests = requestRepository.getDeliveryBoyRequests()
                    .filter { request ->
                        // Only include services that involve pickup
                        (request.serviceID == 1L || request.serviceID == 2L || request.serviceID == 5L) &&
                                // Only include requests currently in pickup-related statuses
                                (request.statusID == 10 || request.statusID == 2)
                    }

                deliveryAdapter?.submit(requests)
                tvNoRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markPickupDone(request: Request) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.markPickupDone(request.requestID)
                if (response.isSuccessful) {
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
