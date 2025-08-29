package com.example.capstone2.adapter

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request

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
        holder.tvSackQty.text = "Sacks: ${request.sackQuantity}"
        holder.tvServices.text = "Services: ${request.serviceName}"
        holder.tvSchedule.text = "Schedule: ${request.schedule ?: "Not set"}"

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
        val tvDetailPaymentMethod: TextView = dialog.findViewById(R.id.tvDetailPaymentMethod)
        val tvDetailPickupLocation: TextView = dialog.findViewById(R.id.tvDetailPickupLocation)
        val tvDetailDeliveryLocation: TextView = dialog.findViewById(R.id.tvDetailDeliveryLocation)
        val tvDetailComment: TextView = dialog.findViewById(R.id.tvDetailComment)
        val tvDetailSubmittedAt: TextView = dialog.findViewById(R.id.tvDetailSubmittedAt)
        val btnClose: Button = dialog.findViewById(R.id.btnClose)
        
        // Set text values
        tvDetailCustomerName.text = "Customer: ${request.customerName}"
        tvDetailSackQty.text = "Sacks: ${request.sackQuantity}"
        tvDetailServices.text = "Services: ${request.serviceName}"
        tvDetailSchedule.text = "Schedule: ${request.schedule ?: "Not set"}"
        tvDetailPaymentMethod.text = "Payment Method: ${request.paymentMethod}"
        
        // Show pickup location if available or if service requires it
        if (!request.pickupLocation.isNullOrEmpty()) {
            tvDetailPickupLocation.text = "Pickup Location: ${request.pickupLocation}"
            tvDetailPickupLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes pickup (serviceID 1,2,5,6)
            when (request.serviceID) {
                1L, 2L, 5L, 6L -> {
                    tvDetailPickupLocation.text = "Pickup Location: Not specified"
                    tvDetailPickupLocation.visibility = View.VISIBLE
                }
                else -> tvDetailPickupLocation.visibility = View.GONE
            }
        }

        // Show delivery location if available or if service requires it
        if (!request.deliveryLocation.isNullOrEmpty()) {
            tvDetailDeliveryLocation.text = "Delivery Location: ${request.deliveryLocation}"
            tvDetailDeliveryLocation.visibility = View.VISIBLE
        } else {
            // Check if service includes delivery (serviceID 1,3,5,7)
            when (request.serviceID) {
                1L, 3L, 5L, 7L -> {
                    tvDetailDeliveryLocation.text = "Delivery Location: Not specified"
                    tvDetailDeliveryLocation.visibility = View.VISIBLE
                }
                else -> tvDetailDeliveryLocation.visibility = View.GONE
            }
        }
        
        tvDetailComment.text = "Comment: ${request.comment ?: "None"}"
        tvDetailSubmittedAt.text = "Submitted At: ${request.submittedAt ?: "Unknown"}"
        
        // Close button handler
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
