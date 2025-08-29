package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request

class CustomerHistoryAdapter(
    private var completedRequests: List<Request>
) : RecyclerView.Adapter<CustomerHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val completionDate: TextView = itemView.findViewById(R.id.tvCompletionDate)
        val serviceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        val detailsLayout: LinearLayout = itemView.findViewById(R.id.layoutDetails)
        
        // Detail view elements
        val requestId: TextView = itemView.findViewById(R.id.tvRequestId)
        val sackQuantity: TextView = itemView.findViewById(R.id.tvSackQuantity)
        val paymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val request = completedRequests[position]
        
        // Set the compact view info
        holder.completionDate.text = "Completed on: ${request.dateUpdated ?: "N/A"}"
        holder.serviceName.text = "Service: ${request.serviceName}"
        
        // Set the detailed view info
        holder.requestId.text = "Request ID: ${request.requestID}"
        holder.sackQuantity.text = "Sack Quantity: ${request.sackQuantity}"
        holder.paymentMethod.text = "Payment Method: ${request.paymentMethod}"
        holder.status.text = "Status: Completed"
        
        // Toggle details visibility on button click
        var isExpanded = false
        
        holder.btnViewDetails.setOnClickListener {
            isExpanded = !isExpanded
            holder.detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.btnViewDetails.text = if (isExpanded) "Hide Details" else "View Details"
        }
    }

    override fun getItemCount() = completedRequests.size

    fun updateRequests(newRequests: List<Request>) {
        completedRequests = newRequests
        notifyDataSetChanged()
    }
} 