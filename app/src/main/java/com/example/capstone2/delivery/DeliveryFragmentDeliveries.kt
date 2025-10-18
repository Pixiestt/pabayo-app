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

class DeliveryFragmentDeliveries : Fragment(R.layout.fragment_delivery_deliveries) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var requestRepository: RequestRepository
    private var deliveryAdapter: DeliveryCardAdapter? = null
    private lateinit var apiService: ApiService

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewDeliveryDeliveries)
        tvNoRequests = view.findViewById(R.id.tvNoRequestsDeliveries)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Get token from SharedPreferences
        val token = SharedPrefManager.getAuthToken(requireContext())
        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize ApiService + Repository
        apiService = ApiClient.getApiService { token }
        requestRepository = RequestRepository(apiService)

        // Initialize adapter
        deliveryAdapter = DeliveryCardAdapter(
            mutableListOf(),
            DeliveryMode.DELIVERIES,
            { request, action ->
                when (action) {
                    DeliveryAction.INITIATE -> markDeliveryInitiated(request)
                    DeliveryAction.DONE -> markDeliveryDone(request)
                }
            },
            requestRepository
        )
        recyclerView.adapter = deliveryAdapter

        fetchDeliveries()
    }

    private fun fetchDeliveries() {
        lifecycleScope.launch {
            try {
                // Show requests ready for or out on delivery: status 12 (Milling done) or 6 (Rider out)
                val requests = requestRepository.getDeliveryBoyRequests()
                    .filter { request ->
                        val deliveryRelatedService = request.serviceID in listOf(1L, 3L, 5L, 7L)
                        val deliveryStatus = request.statusID == 12 || request.statusID == 6
                        (deliveryStatus && deliveryRelatedService)
                    }


                deliveryAdapter?.submit(requests)
                tvNoRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error fetching deliveries: ${e.message}", Toast.LENGTH_SHORT).show()
                tvNoRequests.visibility = View.VISIBLE
            }
        }
    }

    private fun markDeliveryInitiated(request: Request) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.updateRequestStatus(request.requestID, 6) // Rider out
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Delivery initiated", Toast.LENGTH_SHORT).show()
                    fetchDeliveries() // refresh list to update button states
                } else {
                    Toast.makeText(requireContext(), "Failed to initiate delivery", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun markDeliveryDone(request: Request) {
        lifecycleScope.launch {
            try {
                val response = requestRepository.updateRequestStatus(request.requestID, 13) // Delivered
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Delivery completed", Toast.LENGTH_SHORT).show()
                    // Refetch the updated list
                    fetchDeliveries()
                } else {
                    Toast.makeText(requireContext(), "Failed to mark delivery done", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
