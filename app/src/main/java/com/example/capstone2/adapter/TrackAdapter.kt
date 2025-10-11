package com.example.capstone2.adapter

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.user.ViewUserProfileActivity
import com.example.capstone2.data.models.Request

class TrackAdapter(
    private var requests: List<Request>,
    private val onButtonClick: (Request, Int) -> Unit,
    /**
     * Callback to fetch a contact number for a given customerId. Should invoke onResult
     * on the main thread with the resulting contact string (e.g. actual number or "Not available").
     */
    private val fetchContact: (customerId: Long, onResult: (String) -> Unit) -> Unit
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

        val btnSubmit = itemView.findViewById<Button>(R.id.btnSubmit)
        val btnMore = itemView.findViewById<ImageButton>(R.id.btnMore)
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
        holder.tvSchedule.text = ctx.getString(
            R.string.schedule_format_owner,
            req.schedule ?: ctx.getString(R.string.not_set)
        )

        // Hide locations in item row (shown in More dialog)
        holder.tvPickupLocation.visibility = View.GONE
        holder.tvDeliveryLocation.visibility = View.GONE

        // Current status text and color
        val currentStatusInt = try { req.statusID.toInt() } catch (_: Exception) { 0 }
        val currentStatusText = getStatusText(currentStatusInt)
        holder.tvCurrentStatus.text = ctx.getString(R.string.status_format, currentStatusText)

        val statusColor = when (currentStatusInt) {
            10 -> "#4CAF50"
            2, 3 -> "#FF9800"
            4 -> "#2196F3"
            5 -> "#9C27B0"
            6, 7 -> "#FF5722"
            else -> "#3F51B5"
        }
        try {
            holder.tvCurrentStatus.setTextColor(android.graphics.Color.parseColor(statusColor))
        } catch (_: Exception) { /* ignore color parse issues */ }

        // All radio buttons list
        val allRadioButtons = listOf(
            holder.rbDpickup, holder.rbCDropoff, holder.rbPending, holder.rbProcessing,
            holder.rbOutForDelivery, holder.rbCPickup, holder.rbMillingDone, holder.rbDelivered
        )

        // Reset UI state
        allRadioButtons.forEach { rb ->
            rb.visibility = View.GONE
            rb.isChecked = false
            rb.isEnabled = true
            rb.isClickable = true
            rb.setOnClickListener(null)
        }
        holder.rgStatusOptions.setOnCheckedChangeListener(null)
        holder.rgStatusOptions.clearCheck()
        holder.rgStatusOptions.visibility = View.GONE
        holder.rgStatusOptions.isEnabled = false
        holder.btnSubmit.visibility = View.GONE
        holder.btnSubmit.isEnabled = false
        holder.btnSubmit.setOnClickListener(null)

        val serviceId = try { req.serviceID.toInt() } catch (_: Exception) { 0 }
        val currentStatus = currentStatusInt

        val hasPickup = serviceId in listOf(1, 2, 5)
        val hasDelivery = serviceId in listOf(1, 3, 5)

        // Define the canonical progression for different service types
        val fullStatusIDs = when (req.serviceID) {
            1L, 5L -> listOf(2, 4, 5, 6)
            2L, 6L -> listOf(2, 4, 5, 7)
            3L, 7L -> listOf(3, 4, 5, 6)
            4L, 8L -> listOf(3, 4, 5, 7)
            else -> listOf()
        }

        // Compute nextStatus using a combination of the canonical list and a couple of special rules
        var nextStatus: Int? = null
        if (currentStatus == 10) {
            // owner can set 'Waiting for customer drop off' from Request Accepted
            nextStatus = 3
        } else if (currentStatus == 5) {
            // from Processing -> Milling Done
            nextStatus = 12
        } else if (fullStatusIDs.isNotEmpty()) {
            val currentIndex = fullStatusIDs.indexOf(currentStatus)
            nextStatus = when {
                currentIndex >= 0 && currentIndex < fullStatusIDs.size - 1 -> fullStatusIDs[currentIndex + 1]
                currentIndex == -1 -> fullStatusIDs[0]
                else -> null
            }
        }

        val statusToRadioMap = mapOf(
            2 to holder.rbDpickup,
            3 to holder.rbCDropoff,
            4 to holder.rbPending,
            5 to holder.rbProcessing,
            6 to holder.rbOutForDelivery,
            7 to holder.rbCPickup,
            12 to holder.rbMillingDone,
            13 to holder.rbDelivered
        )

        if (nextStatus != null) {
            val nextRadio = statusToRadioMap[nextStatus]
            nextRadio?.let { rb ->
                rb.visibility = View.VISIBLE
                rb.isChecked = false
                rb.isEnabled = true

                holder.rgStatusOptions.visibility = View.VISIBLE
                holder.rgStatusOptions.isEnabled = true
                holder.btnSubmit.visibility = View.VISIBLE
                holder.btnSubmit.isEnabled = false

                rb.setOnClickListener { view ->
                    if (view is RadioButton) {
                        view.isChecked = true
                        holder.rgStatusOptions.check(view.id)
                        holder.btnSubmit.isEnabled = true
                        Log.d("TrackAdapter", "Radio clicked for request=${req.requestID}, radioId=${view.id}")
                    }
                }
            }

            holder.rgStatusOptions.setOnCheckedChangeListener { _, checkedId ->
                holder.btnSubmit.isEnabled = checkedId != -1
            }

            val radioIdToStatus = mapOf(
                holder.rbDpickup.id to 2,
                holder.rbCDropoff.id to 3,
                holder.rbPending.id to 4,
                holder.rbProcessing.id to 5,
                holder.rbOutForDelivery.id to 6,
                holder.rbCPickup.id to 7,
                holder.rbMillingDone.id to 12,
                holder.rbDelivered.id to 13
            )

            holder.btnSubmit.setOnClickListener {
                val checkedId = holder.rgStatusOptions.checkedRadioButtonId
                val selectedStatus = radioIdToStatus[checkedId]
                if (selectedStatus != null) {
                    onButtonClick(req, selectedStatus)
                } else if (nextStatus != null) {
                    // fallback: if user didn't explicitly pick a radio but we computed a next status
                    onButtonClick(req, nextStatus)
                } else {
                    Toast.makeText(holder.itemView.context, "Please select a status option", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // More button shows details dialog and fetches contact
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

            detailsBuilder.append("Comment: ${if (!req.comment.isNullOrEmpty()) req.comment else "None"}\n")

            // Create a custom dialog from layout so we can show contact and action buttons
            val dialog = android.app.Dialog(ctx)
            val dlgView = LayoutInflater.from(ctx).inflate(R.layout.dialog_request_details, null)
            dialog.setContentView(dlgView)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            // Find dialog views
            val tvDetailCustomerName: TextView = dlgView.findViewById(R.id.tvDetailCustomerName)
            val tvDetailSackQty: TextView = dlgView.findViewById(R.id.tvDetailSackQty)
            val tvDetailServices: TextView = dlgView.findViewById(R.id.tvDetailServices)
            val tvDetailSchedule: TextView = dlgView.findViewById(R.id.tvDetailSchedule)
            val tvDetailPickupLocation: TextView = dlgView.findViewById(R.id.tvDetailPickupLocation)
            val tvDetailDeliveryLocation: TextView = dlgView.findViewById(R.id.tvDetailDeliveryLocation)
            val tvDetailComment: TextView = dlgView.findViewById(R.id.tvDetailComment)
            val tvDetailSubmittedAt: TextView = dlgView.findViewById(R.id.tvDetailSubmittedAt)
            val tvDetailContact: TextView = dlgView.findViewById(R.id.tvDetailContact)
            val btnCall: Button = dlgView.findViewById(R.id.btnCall)
            val btnCopy: Button = dlgView.findViewById(R.id.btnCopy)
            val btnMsg: Button = dlgView.findViewById(R.id.btnMessage)
            val btnViewProfile: Button = dlgView.findViewById(R.id.btnViewProfile)
            val btnClose: Button = dlgView.findViewById(R.id.btnClose)
            // Hide progress-related UI for owner dialog (we don't show progress in this 'More' view)
            val tvDetailProgressLabel: TextView? = dlgView.findViewById(R.id.tvDetailProgressLabel)
            val progressBarRequest: ProgressBar? = dlgView.findViewById(R.id.progressBarRequest)
            tvDetailProgressLabel?.visibility = View.GONE
            progressBarRequest?.visibility = View.GONE

            // Populate basic fields
            tvDetailCustomerName.text = ctx.getString(com.example.capstone2.R.string.customer_format, req.customerName)
            tvDetailSackQty.text = ctx.getString(com.example.capstone2.R.string.sacks_format, req.sackQuantity)
            tvDetailServices.text = ctx.getString(com.example.capstone2.R.string.services_format, req.serviceName)
            tvDetailSchedule.text = ctx.getString(com.example.capstone2.R.string.schedule_format_owner, req.schedule ?: ctx.getString(com.example.capstone2.R.string.not_set))
            tvDetailComment.text = ctx.getString(com.example.capstone2.R.string.comment_format, req.comment ?: "None")
            tvDetailSubmittedAt.text = ctx.getString(com.example.capstone2.R.string.submitted_at_format, req.submittedAt ?: "Unknown")

            fun showContactRow(contact: String?) {
                if (contact.isNullOrBlank()) {
                    tvDetailContact.visibility = View.GONE
                    btnCall.visibility = View.GONE
                    btnCopy.visibility = View.GONE
                } else {
                    tvDetailContact.visibility = View.VISIBLE
                    tvDetailContact.text = "Contact number: $contact"
                    // Only enable call/copy actions if the contact looks like a phone number
                    val digits = contact.filter { it.isDigit() }
                    val actionable = digits.length >= 6
                    btnCall.visibility = if (actionable) View.VISIBLE else View.GONE
                    btnCopy.visibility = if (actionable) View.VISIBLE else View.GONE
                }
            }

            // Wire View profile button
            btnViewProfile.visibility = View.VISIBLE
            btnViewProfile.setOnClickListener {
                try {
                    val intent = Intent(ctx, ViewUserProfileActivity::class.java)
                    intent.putExtra("userId", req.customerID)
                    ctx.startActivity(intent)
                } catch (_: Exception) { /* ignore */ }
            }

            // Wire Call and Copy actions
            btnCall.setOnClickListener {
                val phone = req.contactNumber
                if (!phone.isNullOrBlank()) {
                    try {
                        val dial = Intent(Intent.ACTION_DIAL)
                        dial.data = android.net.Uri.parse("tel:$phone")
                        ctx.startActivity(dial)
                    } catch (_: Exception) { Toast.makeText(ctx, "Cannot start dialer", Toast.LENGTH_SHORT).show() }
                }
            }

            btnCopy.setOnClickListener {
                val phone = req.contactNumber
                if (!phone.isNullOrBlank()) {
                    try {
                        val clipboard = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("contact", phone)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(ctx, "Contact copied", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) { Toast.makeText(ctx, "Cannot copy", Toast.LENGTH_SHORT).show() }
                }
            }

            // Close button
            btnClose.setOnClickListener { dialog.dismiss() }

            // Initially show either the static contact if present or a loading state and fetch
            if (!req.contactNumber.isNullOrBlank()) {
                showContactRow(req.contactNumber)
            } else {
                // show loading until fetchContact completes
                tvDetailContact.visibility = View.VISIBLE
                tvDetailContact.text = "Contact number: Loading..."
                btnCall.visibility = View.GONE
                btnCopy.visibility = View.GONE
                fetchContact(req.customerID) { finalContact ->
                    // update UI on main thread
                    (ctx as? android.app.Activity)?.runOnUiThread {
                        showContactRow(finalContact)
                    } ?: showContactRow(finalContact)
                }
            }

            dialog.show()
        }
    }

    override fun getItemCount(): Int = requests.size

    fun updateRequests(newRequests: List<Request>) {
        this.requests = newRequests
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
            12 -> "Milling done"
            13 -> "Delivered"
            else -> "Unknown status"
        }
    }
}
