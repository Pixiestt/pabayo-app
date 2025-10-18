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

class OwnerHistoryAdapter(
    private var completedRequests: List<Request>
) : RecyclerView.Adapter<OwnerHistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val completionDate: TextView = itemView.findViewById(R.id.tvCompletionDate)
        val serviceName: TextView = itemView.findViewById(R.id.tvServiceName)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
        val detailsLayout: LinearLayout = itemView.findViewById(R.id.layoutDetails)
        
        // Detail view elements
        val requestId: TextView = itemView.findViewById(R.id.tvRequestId)
        val customerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val sackQuantity: TextView = itemView.findViewById(R.id.tvSackQuantity)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        // New: Payment amount text view
        val paymentAmount: TextView = itemView.findViewById(R.id.tvPaymentAmount)
        // NEW: Milled kilograms text view
        val milledKg: TextView? = itemView.findViewById(R.id.tvMilledKg)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_owner_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val request = completedRequests[position]
        val ctx = holder.itemView.context

        // Set the compact view info
        val completedDate = request.dateUpdated ?: ctx.getString(R.string.not_available)
        holder.completionDate.text = ctx.getString(R.string.completed_on_format, completedDate)
        holder.serviceName.text = ctx.getString(R.string.service_format, request.serviceName)
        holder.btnViewDetails.text = ctx.getString(R.string.view_details)

        // Set the detailed view info
        holder.requestId.text = ctx.getString(R.string.request_id_format, request.requestID)
        holder.customerName.text = ctx.getString(R.string.customer_format, request.customerName)
        holder.sackQuantity.text = ctx.getString(R.string.sack_qty_format, request.sackQuantity)
        holder.status.text = ctx.getString(R.string.status_format, ctx.getString(R.string.completed_label))

        // Bind payment amount if available
        val amt: Double? = request.paymentAmount
            ?: request.payment?.amount
            ?: request.payment?.amountString?.toDoubleOrNull()
        if (amt != null) {
            holder.paymentAmount.text = ctx.getString(R.string.payment_amount_format, amt)
        } else {
            holder.paymentAmount.text = ctx.getString(R.string.payment_amount_not_set)
        }

        // NEW: Bind milled kilograms if available
        holder.milledKg?.let { tv ->
            val kg = request.milledKg
            if (kg != null) {
                tv.visibility = View.VISIBLE
                tv.text = ctx.getString(R.string.milled_kg_format, kg)
            } else {
                tv.visibility = View.GONE
            }
        }

        // Toggle details visibility on button click
        var isExpanded = false
        
        holder.btnViewDetails.setOnClickListener {
            isExpanded = !isExpanded
            holder.detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            holder.btnViewDetails.text = if (isExpanded) ctx.getString(R.string.hide_details) else ctx.getString(R.string.view_details)
        }
    }

    override fun getItemCount() = completedRequests.size

    fun updateRequests(newRequests: List<Request>) {
        completedRequests = newRequests
        notifyDataSetChanged()
    }
}
