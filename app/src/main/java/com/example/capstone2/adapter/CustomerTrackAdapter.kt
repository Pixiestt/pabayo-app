package com.example.capstone2.adapter

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.SharedPrefManager

class CustomerTrackAdapter(
    private var requests: List<Request>,
    private val onMarkCompleteClick: (Request) -> Unit, // Callback for mark complete button
    private val onEditRequest: (Request) -> Unit // Callback when user wants to edit a request
) : RecyclerView.Adapter<CustomerTrackAdapter.CustomerTrackViewHolder>() {

    inner class CustomerTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestId: TextView = itemView.findViewById(R.id.tvRequestID)
        val dateRequested: TextView = itemView.findViewById(R.id.tvDateRequested)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val btnMarkComplete: Button = itemView.findViewById(R.id.btnMarkComplete)
        val btnMore: Button = itemView.findViewById(R.id.btnMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerTrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_track, parent, false)
        return CustomerTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerTrackViewHolder, position: Int) {
        val request = requests[position]
        val ctx = holder.itemView.context

        holder.requestId.text = ctx.getString(R.string.request_id_format, request.requestID)
        holder.dateRequested.text = ctx.getString(R.string.date_requested_format, request.submittedAt ?: "N/A")

        // Map status IDs to readable names based on the database status table
        val statusText = when (request.statusID) {
            1 -> "Subject for approval"
            2 -> "Delivery boy pickup"
            3 -> "Waiting for customer drop off"
            4 -> "Pending"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer to claim"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
            12 -> "Milling done"
            13 -> "Delivered"
            else -> "Unknown Status"
        }
        holder.status.text = ctx.getString(R.string.status_format, statusText)

        // Mark complete button visibility/handler
        if (request.statusID == 8 || request.statusID == 9) {
            holder.btnMarkComplete.visibility = View.GONE
        } else {
            holder.btnMarkComplete.visibility = View.VISIBLE
            holder.btnMarkComplete.setOnClickListener {
                onMarkCompleteClick(request)
            }
        }

        // More button opens a dialog showing full details
        holder.btnMore.setOnClickListener {
            showDetailsDialog(ctx, request)
        }
    }

    override fun getItemCount() = requests.size

    fun updateRequests(newRequests: List<Request>) {
        // Filter out requests with status ID 8 (Completed)
        requests = newRequests.filter { it.statusID != 8 }
        notifyDataSetChanged()
    }

    private fun showDetailsDialog(context: android.content.Context, request: Request) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.dialog_request_details)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Find views in dialog
        val tvDetailCustomerName: TextView = dialog.findViewById(R.id.tvDetailCustomerName)
        val tvDetailSackQty: TextView = dialog.findViewById(R.id.tvDetailSackQty)
        val tvDetailServices: TextView = dialog.findViewById(R.id.tvDetailServices)
        val tvDetailSchedule: TextView = dialog.findViewById(R.id.tvDetailSchedule)
        val tvDetailPickupLocation: TextView = dialog.findViewById(R.id.tvDetailPickupLocation)
        val tvDetailDeliveryLocation: TextView = dialog.findViewById(R.id.tvDetailDeliveryLocation)
        val tvDetailComment: TextView = dialog.findViewById(R.id.tvDetailComment)
        val tvDetailSubmittedAt: TextView = dialog.findViewById(R.id.tvDetailSubmittedAt)
        val tvDetailProgressLabel: TextView = dialog.findViewById(R.id.tvDetailProgressLabel)
        val progressBar: ProgressBar = dialog.findViewById(R.id.progressBarRequest)
        val btnClose: Button = dialog.findViewById(R.id.btnClose)
        val btnEdit: Button = dialog.findViewById(R.id.btnEdit)
        val btnMessage: Button? = dialog.findViewById(R.id.btnMessage)
        // Ensure View Profile button (present in layout) is hidden for the customer "More" dialog
        val btnViewProfile: Button? = dialog.findViewById(R.id.btnViewProfile)
        btnViewProfile?.visibility = View.GONE
        // New contact controls
        val tvDetailContact: TextView? = dialog.findViewById(R.id.tvDetailContact)

        // Use formatted string resources to populate the dialog
        tvDetailCustomerName.text = context.getString(R.string.customer_format, request.customerName)
        tvDetailSackQty.text = context.getString(R.string.sacks_format, request.sackQuantity)
        tvDetailServices.text = context.getString(R.string.services_format, request.serviceName)
        tvDetailSchedule.text = context.getString(R.string.schedule_format, request.schedule ?: context.getString(R.string.not_set))

        // Show pickup location if available or if service requires it
        if (!request.pickupLocation.isNullOrEmpty()) {
            tvDetailPickupLocation.text = context.getString(R.string.pickup_location_format, request.pickupLocation)
            tvDetailPickupLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes pickup (serviceID 1,2,5,6)
            when (request.serviceID) {
                1L, 2L, 5L, 6L -> {
                    tvDetailPickupLocation.text = context.getString(R.string.pickup_location_format, "Not specified")
                    tvDetailPickupLocation.visibility = View.VISIBLE
                }
                else -> tvDetailPickupLocation.visibility = View.GONE
            }
        }

        // Show delivery location if available or if service requires it
        if (!request.deliveryLocation.isNullOrEmpty()) {
            tvDetailDeliveryLocation.text = context.getString(R.string.delivery_location_format, request.deliveryLocation)
            tvDetailDeliveryLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes delivery (serviceID 1,3,5,7)
            when (request.serviceID) {
                1L, 3L, 5L, 7L -> {
                    tvDetailDeliveryLocation.text = context.getString(R.string.delivery_location_format, "Not specified")
                    tvDetailDeliveryLocation.visibility = View.VISIBLE
                }
                else -> tvDetailDeliveryLocation.visibility = View.GONE
            }
        }

        tvDetailComment.text = context.getString(R.string.comment_format, request.comment ?: "None")
        tvDetailSubmittedAt.text = context.getString(R.string.submitted_at_format, request.submittedAt ?: "Unknown")

        // Contact handling: show contact row when available (Call/Copy removed)
        fun showContact(contact: String?) {
            if (tvDetailContact == null) return
            if (contact.isNullOrBlank()) {
                tvDetailContact.visibility = View.GONE
            } else {
                tvDetailContact.visibility = View.VISIBLE
                tvDetailContact.text = context.getString(com.example.capstone2.R.string.contact_number_format, contact)
            }
        }

        // Helper to find an Activity from any Context by walking ContextWrapper.baseContext
        fun findActivityFromContext(c: android.content.Context): android.app.Activity? {
            var cur: android.content.Context? = c
            while (cur is android.content.ContextWrapper) {
                if (cur is android.app.Activity) return cur
                val next = try { cur.baseContext } catch (_: Exception) { null }
                if (next == null || next === cur) break
                cur = next
            }
            return null
        }

        // Close and edit wiring
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        try {
            val loggedUserId = SharedPrefManager.getUserId(context) ?: -1L
            if (request.statusID == 1 && loggedUserId == request.customerID) {
                btnEdit.visibility = View.VISIBLE
                btnEdit.setOnClickListener {
                    // Invoke the fragment's edit callback so it can start the wizard for result
                    onEditRequest(request)
                    dialog.dismiss()
                }
            } else {
                btnEdit.visibility = View.GONE
            }
        } catch (_: Exception) {
            btnEdit.visibility = View.GONE
        }

        // Wire the Message button for customer-side access (open chat with owner)
        btnMessage?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                // Use centralized SharedPrefManager to get current user id (robust)
                val myId = SharedPrefManager.getUserId(context) ?: -1L

                val otherId = if (myId == request.customerID) request.ownerID else request.customerID

                val activity = context as? FragmentActivity
                if (activity != null) {
                    try {
                        val chatFrag = com.example.capstone2.customer.ChatFragment.newInstance(otherId, null, null)
                        dialog.dismiss()
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.flFragment, chatFrag)
                            .addToBackStack(null)
                            .commit()
                    } catch (e: Exception) {
                        dialog.dismiss()
                        android.widget.Toast.makeText(context, "Unable to open chat: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    dialog.dismiss()
                    android.widget.Toast.makeText(context, "Cannot open chat from this context", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Initially show contact if available
        showContact(request.contactNumber)

        // Compute a progress percentage based on statusID and set progress bar + label
        val progress = when (request.statusID) {
            1 -> 0    // Subject for approval at 0%
            10 -> 15  // Request Accepted
            11 -> 20  // Partially Accepted
            2, 3 -> 30 // Delivery boy pickup OR Waiting for customer drop off
            4 -> 40   // Pending
            5 -> 55   // Processing
            12 -> 70  // Milling done
            6, 7 -> 85 // Rider out for delivery OR Waiting for customer to claim
            13 -> 95  // Delivered
            8 -> 100  // Completed
            9 -> 0    // Rejected
            else -> 0
        }

        // Explicitly show progress UI for customers
        tvDetailProgressLabel.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        progressBar.progress = progress
        tvDetailProgressLabel.text = context.getString(R.string.progress_format, progress)

        dialog.show()
    }
}
