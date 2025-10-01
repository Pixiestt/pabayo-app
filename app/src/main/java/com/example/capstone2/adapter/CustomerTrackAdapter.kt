package com.example.capstone2.adapter

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
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
            4 -> "In Queue"
            5 -> "Processing"
            6 -> "Rider out for delivery"
            7 -> "Waiting for customer pickup"
            8 -> "Completed"
            9 -> "Rejected"
            10 -> "Request Accepted"
            11 -> "Partially Accepted"
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
        val tvDetailPaymentMethod: TextView = dialog.findViewById(R.id.tvDetailPaymentMethod)
        val tvDetailPickupLocation: TextView = dialog.findViewById(R.id.tvDetailPickupLocation)
        val tvDetailDeliveryLocation: TextView = dialog.findViewById(R.id.tvDetailDeliveryLocation)
        val tvDetailComment: TextView = dialog.findViewById(R.id.tvDetailComment)
        val tvDetailSubmittedAt: TextView = dialog.findViewById(R.id.tvDetailSubmittedAt)
        val tvDetailProgressLabel: TextView = dialog.findViewById(R.id.tvDetailProgressLabel)
        val progressBar: ProgressBar = dialog.findViewById(R.id.progressBarRequest)
        val btnClose: Button = dialog.findViewById(R.id.btnClose)

        // Use formatted string resources to populate the dialog
        tvDetailCustomerName.text = context.getString(R.string.customer_format, request.customerName)
        tvDetailSackQty.text = context.getString(R.string.sacks_format, request.sackQuantity)
        tvDetailServices.text = context.getString(R.string.services_format, request.serviceName)
        tvDetailSchedule.text = context.getString(R.string.schedule_format, request.schedule ?: "Not set")
        tvDetailPaymentMethod.text = context.getString(R.string.payment_method_format, request.paymentMethod)

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

        // Compute a progress percentage based on statusID and set progress bar + label
        val progress = when (request.statusID) {
            1 -> 5   // Subject for approval
            10 -> 10 // Request Accepted
            4 -> 30  // In Queue
            5 -> 50  // Processing
            11 -> 45 // Partially Accepted
            2 -> 60  // Delivery boy pickup
            3 -> 60  // Waiting for customer drop off
            6 -> 80  // Rider out for delivery
            7 -> 80  // Waiting for customer pickup
            8 -> 100 // Completed
            9 -> 0   // Rejected
            else -> 0
        }

        progressBar.progress = progress
        tvDetailProgressLabel.text = context.getString(R.string.progress_format, progress)

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
