package com.example.capstone2.adapter

import android.graphics.Color
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
        val customerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val tvSackQty: TextView = itemView.findViewById(R.id.tvSackQty)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        val tvPickupLocation: TextView = itemView.findViewById(R.id.tvPickupLocation)
        val tvDeliveryLocation: TextView = itemView.findViewById(R.id.tvDeliveryLocation)
        val tvCurrentStatus: TextView = itemView.findViewById(R.id.tvCurrentStatus)

        val rgStatusOptions: RadioGroup = itemView.findViewById(R.id.rgStatusOptions)
        val rbDpickup: RadioButton = itemView.findViewById(R.id.rbDpickup)
        val rbCDropoff: RadioButton = itemView.findViewById(R.id.rbCDropoff)
        val rbPending: RadioButton = itemView.findViewById(R.id.rbPending)
        val rbProcessing: RadioButton = itemView.findViewById(R.id.rbProcessing)
        val rbOutForDelivery: RadioButton = itemView.findViewById(R.id.rbOutForDelivery)
        val rbCPickup: RadioButton = itemView.findViewById(R.id.rbCPickup)
        val rbMillingDone: RadioButton = itemView.findViewById(R.id.rbMillingDone)
        val rbDelivered: RadioButton = itemView.findViewById(R.id.rbDelivered)

        val btnSubmit: Button = itemView.findViewById(R.id.btnSubmit)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
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
        holder.tvSchedule.text = ctx.getString(R.string.schedule_format_owner, req.schedule ?: ctx.getString(R.string.not_set))
        holder.tvPickupLocation.visibility = View.GONE
        holder.tvDeliveryLocation.visibility = View.GONE
        holder.tvCurrentStatus.text = ctx.getString(R.string.status_format, getStatusText(req.statusID.toInt()))

        val allRadioButtons = listOf(
            holder.rbDpickup, holder.rbCDropoff, holder.rbPending, holder.rbProcessing,
            holder.rbOutForDelivery, holder.rbCPickup, holder.rbMillingDone, holder.rbDelivered
        )
        allRadioButtons.forEach {
            it.visibility = View.GONE
            it.isChecked = false
        }
        holder.rgStatusOptions.clearCheck()
        holder.btnSubmit.visibility = View.GONE
        holder.btnSubmit.isEnabled = false

        val serviceId = req.serviceID.toInt()
        val currentStatus = req.statusID.toInt()

        val hasPickup = serviceId in listOf(1, 2, 5)
        val hasDelivery = serviceId in listOf(1, 3, 5)

        var nextStatus: Int? = null

        when {
            // === Pickup + Delivery ===
            (serviceId == 1 || serviceId == 5) -> when (currentStatus) {
                4 -> nextStatus = 5
                5 -> nextStatus = 12
                else -> nextStatus = null
            }

            // === Pickup Only ===
            (serviceId == 2) -> when (currentStatus) {
                4 -> nextStatus = 5
                5 -> nextStatus = 12
                12 -> nextStatus = 7
            }

            // === Delivery Only ===
            (serviceId == 3) -> when (currentStatus) {
                10 -> nextStatus = 3 // owner can set "Waiting for customer drop off"
                3 -> nextStatus = 4
                4 -> nextStatus = 5
                5 -> nextStatus = 12
                else -> nextStatus = null
            }
        }

        val statusToRadioMap = mapOf(
            2 to holder.rbDpickup,
            3 to holder.rbCDropoff,
            4 to holder.rbPending,
            5 to holder.rbProcessing,
            6 to holder.rbOutForDelivery,
            7 to holder.rbCPickup,
            12 to holder.rbMillingDone
        )

        if (nextStatus != null) {
            statusToRadioMap[nextStatus]?.apply {
                visibility = View.VISIBLE
                holder.rgStatusOptions.visibility = View.VISIBLE
                holder.btnSubmit.visibility = View.VISIBLE
                setOnClickListener {
                    isChecked = true
                    holder.btnSubmit.isEnabled = true
                }
                holder.btnSubmit.setOnClickListener {
                    onButtonClick(req, nextStatus)
                }
            }
        }

        holder.btnMore.setOnClickListener {
            val detailsBuilder = StringBuilder()
            if (!req.pickupLocation.isNullOrEmpty()) {
                detailsBuilder.append("Pickup location: ${req.pickupLocation}\n")
            } else if (hasPickup) {
                detailsBuilder.append("Pickup location: Not set\n")
            }
            if (!req.deliveryLocation.isNullOrEmpty()) {
                detailsBuilder.append("Delivery location: ${req.deliveryLocation}\n")
            } else if (hasDelivery) {
                detailsBuilder.append("Delivery location: Not set\n")
            }
            detailsBuilder.append("Comment: ${req.comment ?: "None"}\n")
            val contact = if (!req.contactNumber.isNullOrEmpty()) req.contactNumber else "Not available"
            detailsBuilder.append("Contact number: $contact")

            android.app.AlertDialog.Builder(ctx)
                .setTitle("Details")
                .setMessage(detailsBuilder.toString().trim())
                .setPositiveButton("Close", null)
                .show()
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateRequests(newRequests: List<Request>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    private fun getStatusText(statusId: Int): String = when (statusId) {
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
        12 -> "Milling done"
        13 -> "Delivered"
        else -> "Unknown status"
    }
}
