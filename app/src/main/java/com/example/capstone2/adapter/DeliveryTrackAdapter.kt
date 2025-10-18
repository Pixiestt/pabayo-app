package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request

class DeliveryTrackAdapter(
    private var requests: List<Request>,
    private val onMarkCompleteClick: (Request) -> Unit,
    private val onMoreClick: (Request) -> Unit
) : RecyclerView.Adapter<DeliveryTrackAdapter.DeliveryTrackViewHolder>() {

    inner class DeliveryTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestId: TextView = itemView.findViewById(R.id.tvRequestIDDelivery)
        val dateRequested: TextView = itemView.findViewById(R.id.tvDateRequestedDelivery)
        val status: TextView = itemView.findViewById(R.id.tvStatusDelivery)
        val btnMarkComplete: Button = itemView.findViewById(R.id.btnMarkCompleteDelivery)
        val btnMore: Button = itemView.findViewById(R.id.btnMoreDelivery)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeliveryTrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivery_track, parent, false)
        return DeliveryTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeliveryTrackViewHolder, position: Int) {
        val item = requests[position]
        val ctx = holder.itemView.context

        holder.requestId.text = ctx.getString(R.string.request_id_format, item.requestID)
        holder.dateRequested.text = ctx.getString(R.string.date_requested_format, item.submittedAt ?: "N/A")

        val statusText = when (item.statusID) {
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

        holder.btnMore.setOnClickListener { onMoreClick(item) }
        holder.btnMarkComplete.setOnClickListener { onMarkCompleteClick(item) }
    }

    override fun getItemCount(): Int = requests.size

    fun submitList(newRequests: List<Request>) {
        this.requests = newRequests
        notifyDataSetChanged()
    }
}
