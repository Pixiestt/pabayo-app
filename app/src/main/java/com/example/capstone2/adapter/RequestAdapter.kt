package com.example.capstone2.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.SharedPrefManager

class RequestAdapter(
    private var requestList: List<Request>,
    private val onButtonClick: (Request) -> Unit
) : RecyclerView.Adapter<RequestAdapter.RequestViewHolder>() {

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSackQty: TextView = itemView.findViewById(R.id.tvSackQty)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        val btnAction: Button = itemView.findViewById(R.id.btnAction)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_request_simplified, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requestList[position]
        val ctx = holder.itemView.context
        holder.tvSackQty.text = ctx.getString(R.string.sacks_format, request.sackQuantity)
        holder.tvServices.text = ctx.getString(R.string.services_format, request.serviceName)
        holder.tvSchedule.text = ctx.getString(
            R.string.schedule_format,
            request.schedule ?: ctx.getString(R.string.not_set)
        )

        // Set up action button click handler
        holder.btnAction.setOnClickListener {
            onButtonClick(request)
        }
        
        // Set up view details button click handler
        holder.btnViewDetails.setOnClickListener {
            showDetailsDialog(holder.itemView.context, request)
        }
    }

    override fun getItemCount(): Int = requestList.size

    fun updateRequests(newRequests: List<Request>) {
        requestList = newRequests
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
        val btnClose: Button = dialog.findViewById(R.id.btnClose)
        val btnMessage: Button? = dialog.findViewById(R.id.btnMessage)

        // Set text values
        tvDetailCustomerName.text = context.getString(R.string.customer_format, request.customerName)
        tvDetailSackQty.text = context.getString(R.string.sacks_format, request.sackQuantity)
        tvDetailServices.text = context.getString(R.string.services_format, request.serviceName)
        tvDetailSchedule.text = context.getString(
            R.string.schedule_format,
            request.schedule ?: context.getString(R.string.not_set)
        )

        if (!request.pickupLocation.isNullOrEmpty()) {
            tvDetailPickupLocation.text = context.getString(R.string.pickup_location_format, request.pickupLocation)
            tvDetailPickupLocation.visibility = View.VISIBLE
        } else {
            when (request.serviceID) {
                1L, 2L, 5L, 6L -> {
                    tvDetailPickupLocation.text = context.getString(R.string.pickup_location_format, "Not specified")
                    tvDetailPickupLocation.visibility = View.VISIBLE
                }
                else -> tvDetailPickupLocation.visibility = View.GONE
            }
        }

        if (!request.deliveryLocation.isNullOrEmpty()) {
            tvDetailDeliveryLocation.text = context.getString(R.string.delivery_location_format, request.deliveryLocation)
            tvDetailDeliveryLocation.visibility = View.VISIBLE
        } else {
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

        // Wire the Message button so owner/customer can start 1:1 chat
        btnMessage?.let { btn ->
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                // Use centralized SharedPrefManager to get current user id
                val myId = SharedPrefManager.getUserId(context)

                // Decide who is the "other" user in chat
                val otherId = when {
                    myId == null -> request.ownerID // fallback to owner as the other party
                    myId == request.ownerID -> request.customerID
                    myId == request.customerID -> request.ownerID
                    else -> request.customerID
                }

                // Pass the customer name as the chat title when owner opens chat
                val otherName = request.customerName

                // Create chat fragment and navigate using FragmentActivity's supportFragmentManager
                val activity = context as? FragmentActivity
                if (activity != null) {
                    try {
                        val chatFrag = com.example.capstone2.customer.ChatFragment.newInstance(otherId, null, otherName)
                        // Dismiss dialog before navigating
                        dialog.dismiss()
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.flFragment, chatFrag)
                            .addToBackStack(null)
                            .commit()
                    } catch (e: Exception) {
                        // If newInstance signature changed, try alternative construction
                        dialog.dismiss()
                        android.widget.Toast.makeText(context, "Unable to open chat: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    dialog.dismiss()
                    android.widget.Toast.makeText(context, "Cannot open chat from this context", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Close button handler
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
