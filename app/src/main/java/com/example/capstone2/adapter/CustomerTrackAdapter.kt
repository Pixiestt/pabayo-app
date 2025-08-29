package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request

class CustomerTrackAdapter(
    private var requests: List<Request>,
    private val onMarkCompleteClick: (Request) -> Unit // Callback for mark complete button
) : RecyclerView.Adapter<CustomerTrackAdapter.CustomerTrackViewHolder>() {

    inner class CustomerTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestId: TextView = itemView.findViewById(R.id.tvRequestID)
        val dateRequested: TextView = itemView.findViewById(R.id.tvDateRequested)
        val sackQty: TextView = itemView.findViewById(R.id.tvSackQty)
        val services: TextView = itemView.findViewById(R.id.tvServices)
        val pickupLocation: TextView = itemView.findViewById(R.id.tvPickupLocation)
        val deliveryLocation: TextView = itemView.findViewById(R.id.tvDeliveryLocation)
        val paymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val btnMarkComplete: Button = itemView.findViewById(R.id.btnMarkComplete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerTrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_track, parent, false)
        return CustomerTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerTrackViewHolder, position: Int) {
        val request = requests[position]
        
        holder.requestId.text = "Request ID: ${request.requestID}"
        holder.dateRequested.text = "Date Requested: ${request.submittedAt ?: "N/A"}"
        holder.sackQty.text = "Sack Quantity: ${request.sackQuantity}"
        holder.services.text = "Service: ${request.serviceName}"
        
        // Handle pickup location
        if (!request.pickupLocation.isNullOrEmpty()) {
            holder.pickupLocation.text = "Pickup Location: ${request.pickupLocation}"
            holder.pickupLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes pickup (serviceID 1,2,5,6)
            when (request.serviceID) {
                1L, 2L, 5L, 6L -> {
                    holder.pickupLocation.text = "Pickup Location: Not specified"
                    holder.pickupLocation.visibility = View.VISIBLE
                }
                else -> holder.pickupLocation.visibility = View.GONE
            }
        }

        // Handle delivery location
        if (!request.deliveryLocation.isNullOrEmpty()) {
            holder.deliveryLocation.text = "Delivery Location: ${request.deliveryLocation}"
            holder.deliveryLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes delivery (serviceID 1,3,5,7)
            when (request.serviceID) {
                1L, 3L, 5L, 7L -> {
                    holder.deliveryLocation.text = "Delivery Location: Not specified"
                    holder.deliveryLocation.visibility = View.VISIBLE
                }
                else -> holder.deliveryLocation.visibility = View.GONE
            }
        }
        
        holder.paymentMethod.text = "Payment Method: ${request.paymentMethod}"
        
        // Map status IDs to readable names based on the database status table
        val statusText = when (request.statusID) {
            1 -> "Subject for approval"
            2 -> "Delivery boy pickup"
            3 -> "Waiting for customer drop off"
            4 -> "Pending"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer pickup"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
            else -> "Unknown Status"
        }
        holder.status.text = "Status: $statusText"
        
        // Only show the Mark Complete button for active requests (not completed or rejected)
        if (request.statusID == 8 || request.statusID == 9) {
            holder.btnMarkComplete.visibility = View.GONE
        } else {
            holder.btnMarkComplete.visibility = View.VISIBLE
            holder.btnMarkComplete.setOnClickListener {
                onMarkCompleteClick(request)
            }
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<Request>) {
        // Filter out requests with status ID 8 (Completed)
        requests = newRequests.filter { it.statusID != 8 }
        notifyDataSetChanged()
    }
} 