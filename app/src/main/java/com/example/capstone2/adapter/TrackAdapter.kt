package com.example.capstone2.adapter

import android.util.Log
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
        // More button we added to show extra details (use ImageButton to match layout)
        val btnMore = itemView.findViewById<android.widget.ImageButton>(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val req = requests[position]
        val ctx = holder.itemView.context
        holder.customerName.text = req.customerName
        holder.tvSackQty.text = ctx.getString(R.string.sacks_format, req.sackQuantity)
        holder.tvServices.text = ctx.getString(R.string.services_format, req.serviceName)
        // Owner track should display the shorter label 'Schedule:' per request
        holder.tvSchedule.text = ctx.getString(R.string.schedule_format_owner, req.schedule ?: ctx.getString(R.string.not_set))

        // Pickup location is shown in the More dialog only; hide it in the item view
        holder.tvPickupLocation.visibility = View.GONE

        // Delivery location is shown in the More dialog only; hide it in the item view
        holder.tvDeliveryLocation.visibility = View.GONE

        // Set current status text
        val currentStatusText = getStatusText(req.statusID.toInt())
        holder.tvCurrentStatus.text = ctx.getString(R.string.status_format, currentStatusText)

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

        // Reset: hide and uncheck all radio buttons and hide radio group/submit by default
        allRadioButtons.forEach {
            it.visibility = View.GONE
            it.isChecked = false
            it.isEnabled = true
            it.isClickable = true
            it.isFocusable = true
            it.setOnClickListener(null) // clear any previous click listeners
        }
        // clear previous group listener before resetting
        holder.rgStatusOptions.setOnCheckedChangeListener(null)
        holder.rgStatusOptions.clearCheck()
        holder.rgStatusOptions.visibility = View.GONE
        holder.rgStatusOptions.isEnabled = false
        holder.rgStatusOptions.isClickable = false
        holder.btnSubmit.visibility = View.GONE
        holder.btnSubmit.isEnabled = false
        holder.btnSubmit.setOnClickListener(null) // clear previous submit listener

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

        // Determine next status to show (handle case when status isn't in flow, e.g., 10 = Accepted)
        val nextStatus: Int? = when {
            fullStatusIDs.isEmpty() -> null
            currentIndex >= 0 && currentIndex < fullStatusIDs.size - 1 -> fullStatusIDs[currentIndex + 1]
            currentIndex == -1 -> fullStatusIDs[0] // start flow when current status is 'Accepted' or outside flow
            else -> null
        }

        if (nextStatus != null) {
            // Show only the radio corresponding to the next status
            val nextStepRadioButton = statusToRadioMap[nextStatus]
            nextStepRadioButton?.let {
                it.visibility = View.VISIBLE
                it.isChecked = false // do NOT auto-check; allow user to tap
                it.isEnabled = true
                it.isClickable = true
                it.isFocusable = true
                holder.rgStatusOptions.visibility = View.VISIBLE
                holder.rgStatusOptions.isEnabled = true
                holder.rgStatusOptions.isClickable = true
                holder.btnSubmit.visibility = View.VISIBLE
                holder.btnSubmit.isEnabled = false

                // Ensure the parent/item and RadioGroup allow children to receive touch/focus
                // Make the parent not intercept touches so RadioButton receives the first tap
                holder.itemView.isClickable = false
                holder.itemView.isFocusable = false
                holder.itemView.isFocusableInTouchMode = false
                holder.rgStatusOptions.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

                // Add an explicit click listener to ensure immediate selection
                it.setOnClickListener { rb ->
                    (rb as? RadioButton)?.isChecked = true
                    // ensure the radio group registers the change
                    holder.rgStatusOptions.check((rb as RadioButton).id)
                    holder.btnSubmit.isEnabled = true
                    Log.d("TrackAdapter", "Radio clicked for request=${req.requestID}, radioId=${(rb as RadioButton).id}")
                }
            }

            // Enable submit only when a selection is made
            holder.rgStatusOptions.setOnCheckedChangeListener { group, checkedId ->
                holder.btnSubmit.isEnabled = checkedId != -1
                Log.d("TrackAdapter", "Checked changed request=${req.requestID}, checkedId=$checkedId")
            }

            // Map radio id -> status for submit
            val radioIdToStatus = mapOf(
                holder.rbDpickup.id to 2,
                holder.rbCDropoff.id to 3,
                holder.rbPending.id to 4,
                holder.rbProcessing.id to 5,
                holder.rbOutForDelivery.id to 6,
                holder.rbCPickup.id to 7
            )

            // Add submit button click listener that reads the selected radio at the moment of click
            holder.btnSubmit.setOnClickListener {
                val checkedId = holder.rgStatusOptions.checkedRadioButtonId
                val selectedStatus = radioIdToStatus[checkedId]
                Log.d("TrackAdapter", "Submit clicked request=${req.requestID}, checkedId=$checkedId, selectedStatus=$selectedStatus")
                if (selectedStatus != null) {
                    onButtonClick(req, selectedStatus)
                } else {
                    Toast.makeText(holder.itemView.context, "Please select a status option", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // No next step available -> hide submit (already hidden by reset above)
            holder.btnSubmit.visibility = View.GONE
            holder.btnSubmit.setOnClickListener(null)
            holder.rgStatusOptions.setOnCheckedChangeListener(null)
        }

        // Wire the More button to show a dialog with extra details
        holder.btnMore.setOnClickListener {
            val detailsBuilder = StringBuilder()

            // Include pickup location if visible or service requires pickup
            if (!req.pickupLocation.isNullOrEmpty()) {
                detailsBuilder.append("Pickup location: ${req.pickupLocation}\n")
            } else {
                when (req.serviceID) {
                    1L, 2L, 5L, 6L -> detailsBuilder.append("Pickup location: Not set\n")
                }
            }

            // Include delivery location if visible or service requires delivery
            if (!req.deliveryLocation.isNullOrEmpty()) {
                detailsBuilder.append("Delivery location: ${req.deliveryLocation}\n")
            } else {
                when (req.serviceID) {
                    1L, 3L, 5L, 7L -> detailsBuilder.append("Delivery location: Not set\n")
                }
            }

            // Comment
            detailsBuilder.append("Comment: ${if (!req.comment.isNullOrEmpty()) req.comment else "None"}\n")

            // Contact number — use Request.contactNumber if available
            val contact = if (!req.contactNumber.isNullOrEmpty()) req.contactNumber else "Not available"
            detailsBuilder.append("Contact number: $contact")

            val message = detailsBuilder.toString().trim()

            // Show dialog
            android.app.AlertDialog.Builder(ctx)
                .setTitle("Details")
                .setMessage(if (message.isNotEmpty()) message else "No additional details")
                .setPositiveButton("Close", null)
                .show()
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
            4 -> "In queue"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer to claim"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
            else -> "Unknown status"
        }
    }
}
