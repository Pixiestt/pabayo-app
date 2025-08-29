package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request

class TrackAdapter(
    private var requests: List<Request>,
    private val onButtonClick: (Request, Int) -> Unit
) : RecyclerView.Adapter<TrackAdapter.TrackViewHolder>() {

    inner class TrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val customerName = itemView.findViewById<TextView>(R.id.tvCustomerName)
        val tvSackQty: TextView = itemView.findViewById(R.id.tvSackQty)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        val tvPickupLocation: TextView = itemView.findViewById(R.id.tvPickupLocation)
        val tvDeliveryLocation: TextView = itemView.findViewById(R.id.tvDeliveryLocation)
        val tvCurrentStatus: TextView = itemView.findViewById(R.id.tvCurrentStatus)

        val rgStatusOptions = itemView.findViewById<RadioGroup>(R.id.rgStatusOptions)
        val rbDpickup = itemView.findViewById<RadioButton>(R.id.rbDpickup)
        val rbCDropoff = itemView.findViewById<RadioButton>(R.id.rbCDropoff)
        val rbPending = itemView.findViewById<RadioButton>(R.id.rbPending)
        val rbProcessing = itemView.findViewById<RadioButton>(R.id.rbProcessing)
        val rbOutForDelivery = itemView.findViewById<RadioButton>(R.id.rbOutForDelivery)
        val rbCPickup = itemView.findViewById<RadioButton>(R.id.rbCPickup)

        val btnSubmit = itemView.findViewById<Button>(R.id.btnSubmit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val req = requests[position]
        holder.customerName.text = req.customerName
        holder.tvSackQty.text = "Sacks: ${req.sackQuantity}"
        holder.tvServices.text = "Services: ${req.serviceName}"
        holder.tvSchedule.text = "Schedule: ${req.schedule}"

        // Handle pickup location
        if (!req.pickupLocation.isNullOrEmpty()) {
            holder.tvPickupLocation.text = "Pickup Location: ${req.pickupLocation}"
            holder.tvPickupLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes pickup (serviceID 1,2,5,6)
            when (req.serviceID) {
                1L, 2L, 5L, 6L -> {
                    holder.tvPickupLocation.text = "Pickup Location: Not specified"
                    holder.tvPickupLocation.visibility = View.VISIBLE
                }
                else -> holder.tvPickupLocation.visibility = View.GONE
            }
        }

        // Handle delivery location
        if (!req.deliveryLocation.isNullOrEmpty()) {
            holder.tvDeliveryLocation.text = "Delivery Location: ${req.deliveryLocation}"
            holder.tvDeliveryLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes delivery (serviceID 1,3,5,7)
            when (req.serviceID) {
                1L, 3L, 5L, 7L -> {
                    holder.tvDeliveryLocation.text = "Delivery Location: Not specified"
                    holder.tvDeliveryLocation.visibility = View.VISIBLE
                }
                else -> holder.tvDeliveryLocation.visibility = View.GONE
            }
        }

        // Set current status text
        val currentStatusText = getStatusText(req.statusID.toInt())
        holder.tvCurrentStatus.text = "Current Status: $currentStatusText"
        
        // Set a color based on status
        val statusColor = when(req.statusID.toInt()) {
            10 -> "#4CAF50" // Green for accepted
            2, 3 -> "#FF9800" // Orange for pickup/dropoff
            4 -> "#2196F3" // Blue for pending
            5 -> "#9C27B0" // Purple for processing
            6, 7 -> "#FF5722" // Deep orange for delivery/pickup
            else -> "#3F51B5" // Indigo default
        }
        holder.tvCurrentStatus.setTextColor(android.graphics.Color.parseColor(statusColor))

        val statusToRadioMap = mapOf(
            2 to holder.rbDpickup,
            3 to holder.rbCDropoff,
            4 to holder.rbPending,
            5 to holder.rbProcessing,
            6 to holder.rbOutForDelivery,
            7 to holder.rbCPickup
        )

        val allRadioButtons = listOf(
            holder.rbDpickup, holder.rbCDropoff, holder.rbPending,
            holder.rbProcessing, holder.rbOutForDelivery, holder.rbCPickup
        )

        // Hide and uncheck all radio buttons
        allRadioButtons.forEach {
            it.visibility = View.GONE
            it.isChecked = false
        }

        // Define full step list based on serviceID
        val fullStatusIDs = when (req.serviceID) {
            1L, 5L -> listOf(2, 4, 5, 6)
            2L, 6L -> listOf(2, 4, 5, 7)
            3L, 7L -> listOf(3, 4, 5, 6)
            4L, 8L -> listOf(3, 4, 5, 7)
            else -> listOf()
        }
        
        // Find current index in flow
        val currentIndex = fullStatusIDs.indexOf(req.statusID.toInt())
        
        // Only show next steps
        if (currentIndex >= 0 && currentIndex < fullStatusIDs.size - 1) {
            val nextStatus = fullStatusIDs[currentIndex + 1]
            val nextStepRadioButton = statusToRadioMap[nextStatus]
            nextStepRadioButton?.visibility = View.VISIBLE
            
            // Add submit button click listener
            holder.btnSubmit.setOnClickListener {
                if (nextStepRadioButton?.isChecked == true) {
                    onButtonClick(req, nextStatus)
                } else {
                    Toast.makeText(holder.itemView.context, "Please select a status option", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            holder.btnSubmit.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateRequests(newRequests: List<Request>) {
        requests = newRequests
        notifyDataSetChanged()
    }
    
    private fun getStatusText(statusId: Int): String {
        return when (statusId) {
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
            else -> "Unknown status"
        }
    }
}
