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
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.util.StatusColorProvider
import com.example.capstone2.util.StatusNameProvider
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
        val tvRequestId: TextView = itemView.findViewById(R.id.tvRequestId)
        val customerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        val tvSackQty: TextView = itemView.findViewById(R.id.tvSackQty)
        val tvServices: TextView = itemView.findViewById(R.id.tvServices)
        val tvSchedule: TextView = itemView.findViewById(R.id.tvSchedule)
        val tvPickupLocation: TextView = itemView.findViewById(R.id.tvPickupLocation)
        val tvDeliveryLocation: TextView = itemView.findViewById(R.id.tvDeliveryLocation)
        val tvCurrentStatus: TextView = itemView.findViewById(R.id.tvCurrentStatus)
        val tvPaymentAmount: TextView? = itemView.findViewById(R.id.tvPaymentAmount)
        val tvMilledKg: TextView? = itemView.findViewById(R.id.tvMilledKg)
        val tvPickupPreparingMessage: TextView? = itemView.findViewById(R.id.tvPickupPreparingMessage)

        val rgStatusOptions: RadioGroup = itemView.findViewById(R.id.rgStatusOptions)
        val rbDpickup: RadioButton = itemView.findViewById(R.id.rbDpickup)
        val rbCDropoff: RadioButton = itemView.findViewById(R.id.rbCDropoff)
        val rbPending: RadioButton = itemView.findViewById(R.id.rbPending)
        val rbProcessing: RadioButton = itemView.findViewById(R.id.rbProcessing)
        val rbOutForDelivery: RadioButton = itemView.findViewById(R.id.rbOutForDelivery)
        val rbCPickup: RadioButton = itemView.findViewById(R.id.rbCPickup)
        val rbMillingDone: RadioButton = itemView.findViewById(R.id.rbMillingDone)
        val rbDelivered: RadioButton = itemView.findViewById(R.id.rbDelivered)

        // Action buttons present in the owner item layout
        val btnSubmit: Button = itemView.findViewById(R.id.btnSubmit)
        val btnMore: ImageButton = itemView.findViewById(R.id.btnMore)
        val btnRowMessage: Button? = itemView.findViewById(R.id.btnRowMessage)
    }

    // Helper to parse formatted currency strings like "₱1,234.50" safely
    private fun parseAmountStringSafe(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(Regex("[^0-9.,-]"), "").replace(",", "")
        return cleaned.toDoubleOrNull()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        return TrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        val req = requests[position]
        val ctx = holder.itemView.context

        // Show Request ID at upper-left
        holder.tvRequestId.text = ctx.getString(R.string.request_id_format, req.requestID)

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
        val currentStatusInt = try { req.statusID } catch (_: Exception) { 0 }
        val currentStatusText = StatusNameProvider.getNameFor(currentStatusInt)
        holder.tvCurrentStatus.text = ctx.getString(R.string.status_format, currentStatusText)
        try {
            val colorInt = StatusColorProvider.getColorFor(currentStatusInt)
            holder.tvCurrentStatus.setTextColor(colorInt)
        } catch (_: Exception) { /* ignore */ }

        // NEW: Payment amount line for owner row if available
        holder.tvPaymentAmount?.let { tv ->
            val amt = req.paymentAmount
                ?: req.payment?.amount
                ?: parseAmountStringSafe(req.payment?.amountString)
            if (amt != null && amt >= 0.0) {
                tv.visibility = View.VISIBLE
                tv.text = ctx.getString(R.string.payment_amount_format, amt)
            } else {
                tv.visibility = View.GONE
            }
        }

        // NEW: milled kg if available
        holder.tvMilledKg?.let { tv ->
            val kg = req.milledKg
            if (kg != null && kg >= 0.0) {
                tv.visibility = View.VISIBLE
                tv.text = ctx.getString(R.string.milled_kg_format, kg)
            } else {
                tv.visibility = View.GONE
            }
        }

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
        holder.btnRowMessage?.visibility = View.GONE
        holder.btnRowMessage?.setOnClickListener(null)

        val currentStatus = currentStatusInt

        val includesPickup = req.serviceID in listOf(1L, 2L, 5L, 6L)
        val includesDelivery = req.serviceID in listOf(1L, 3L, 5L, 7L)
        // 12 (Milling done) should not be terminal for non-delivery services because we need to move to 7 (Waiting for customer to claim)
        val terminalStatuses = setOf(8, 9, 13)

        // Compute next status strictly following owner-side rules
        val nextStatus: Int? = when {
            currentStatus in terminalStatuses -> null
            // New rule: If there's no delivery and we're at Milling done, next is Waiting for customer to claim (7)
            !includesDelivery && currentStatus == 12 -> 7
            includesPickup -> when (currentStatus) {
                10 -> null
                4 -> 5
                5 -> 12
                else -> null
            }
            else -> when (currentStatus) {
                10 -> 3
                3 -> 4
                4 -> 5
                5 -> 12
                else -> null
            }
        }

        // Show or hide pickup preparing message
        holder.tvPickupPreparingMessage?.visibility = if (includesPickup && currentStatus == 10 && nextStatus == null) View.VISIBLE else View.GONE

        val statusToRadioMap = mapOf(
            3 to holder.rbCDropoff,
            4 to holder.rbPending,
            5 to holder.rbProcessing,
            7 to holder.rbCPickup,
            12 to holder.rbMillingDone
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
                holder.rbCDropoff.id to 3,
                holder.rbPending.id to 4,
                holder.rbProcessing.id to 5,
                holder.rbCPickup.id to 7,
                holder.rbMillingDone.id to 12
            )

            holder.btnSubmit.setOnClickListener {
                val checkedId = holder.rgStatusOptions.checkedRadioButtonId
                val selectedStatus = radioIdToStatus[checkedId] ?: nextStatus
                if (selectedStatus != null) {
                    onButtonClick(req, selectedStatus)
                } else {
                    Toast.makeText(holder.itemView.context, R.string.submit, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Row-level Message button: show only when Waiting for customer to claim (status 7)
        if (currentStatus == 7) {
            holder.btnRowMessage?.visibility = View.VISIBLE
            holder.btnRowMessage?.setOnClickListener {
                val context = holder.itemView.context
                val myId = SharedPrefManager.getUserId(context)
                val otherId = when {
                    myId == null -> req.customerID
                    myId == req.ownerID -> req.customerID
                    else -> req.customerID
                }
                val otherName = req.customerName
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    try {
                        val chatFrag = com.example.capstone2.customer.ChatFragment.newInstance(otherId, null, otherName)
                        activity.supportFragmentManager.beginTransaction()
                            .replace(R.id.flFragment, chatFrag)
                            .addToBackStack(null)
                            .commit()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open chat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Cannot open chat from this context", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // More button shows details dialog and fetches contact
        holder.btnMore.setOnClickListener {
            val detailsBuilder = StringBuilder()

            if (!req.pickupLocation.isNullOrEmpty()) {
                detailsBuilder.append("Pickup location: ${req.pickupLocation}\n")
            } else if (includesPickup) {
                detailsBuilder.append("Pickup location: Not set\n")
            }

            if (!req.deliveryLocation.isNullOrEmpty()) {
                detailsBuilder.append("Delivery location: ${req.deliveryLocation}\n")
            } else if (includesDelivery) {
                detailsBuilder.append("Delivery location: Not set\n")
            }

            detailsBuilder.append("Comment: ${if (!req.comment.isNullOrEmpty()) req.comment else "None"}\n")

            // Create a custom dialog from layout so we can show contact and action buttons
            val dialog = android.app.Dialog(ctx)
            val dlgView = LayoutInflater.from(ctx).inflate(R.layout.dialog_request_details, null, false)
            dialog.setContentView(dlgView)
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

            // Find dialog views
            val tvDetailCustomerName: TextView = dlgView.findViewById(R.id.tvDetailCustomerName)
            val tvDetailSackQty: TextView = dlgView.findViewById(R.id.tvDetailSackQty)
            val tvDetailServices: TextView = dlgView.findViewById(R.id.tvDetailServices)
            val tvDetailSchedule: TextView = dlgView.findViewById(R.id.tvDetailSchedule)
            val tvDetailComment: TextView = dlgView.findViewById(R.id.tvDetailComment)
            val tvDetailSubmittedAt: TextView = dlgView.findViewById(R.id.tvDetailSubmittedAt)
            val tvDetailContact: TextView = dlgView.findViewById(R.id.tvDetailContact)
            val tvDetailPaymentAmount: TextView = dlgView.findViewById(R.id.tvDetailPaymentAmount)
            val tvDetailMilledKg: TextView = dlgView.findViewById(R.id.tvDetailMilledKg)
            val btnMsg: Button = dlgView.findViewById(R.id.btnMessage)
            val btnViewProfile: Button = dlgView.findViewById(R.id.btnViewProfile)
            val btnMarkComplete: Button = dlgView.findViewById(R.id.btnMarkComplete)
            val btnClose: Button = dlgView.findViewById(R.id.btnClose)
            val tvDetailProgressLabel: TextView? = dlgView.findViewById(R.id.tvDetailProgressLabel)
            val progressBarRequest: ProgressBar? = dlgView.findViewById(R.id.progressBarRequest)
            tvDetailProgressLabel?.visibility = View.GONE
            progressBarRequest?.visibility = View.GONE

            // Populate basic fields
            tvDetailCustomerName.text = ctx.getString(R.string.customer_format, req.customerName)
            tvDetailSackQty.text = ctx.getString(R.string.sacks_format, req.sackQuantity)
            tvDetailServices.text = ctx.getString(R.string.services_format, req.serviceName)
            tvDetailSchedule.text = ctx.getString(R.string.schedule_format_owner, req.schedule ?: ctx.getString(R.string.not_set))
            tvDetailComment.text = ctx.getString(R.string.comment_format, req.comment ?: "None")
            tvDetailSubmittedAt.text = ctx.getString(R.string.submitted_at_format, req.submittedAt ?: "Unknown")

            // Payment amount in dialog
            val amt = req.paymentAmount
                ?: req.payment?.amount
                ?: parseAmountStringSafe(req.payment?.amountString)
            if (amt != null && amt >= 0.0) {
                tvDetailPaymentAmount.visibility = View.VISIBLE
                tvDetailPaymentAmount.text = ctx.getString(R.string.payment_amount_format, amt)
            } else {
                tvDetailPaymentAmount.visibility = View.VISIBLE
                tvDetailPaymentAmount.text = ctx.getString(R.string.payment_amount_not_set)
            }

            // Milled kg in dialog
            val kg = req.milledKg
            if (kg != null && kg >= 0.0) {
                tvDetailMilledKg.visibility = View.VISIBLE
                tvDetailMilledKg.text = ctx.getString(R.string.milled_kg_format, kg)
            } else {
                tvDetailMilledKg.visibility = View.GONE
            }

            // Initially show either the static contact if present or a loading state and fetch
            if (!req.contactNumber.isNullOrBlank()) {
                tvDetailContact.visibility = View.VISIBLE
                tvDetailContact.text = ctx.getString(R.string.contact_number_format, req.contactNumber)
            } else {
                tvDetailContact.visibility = View.VISIBLE
                tvDetailContact.text = ctx.getString(R.string.contact_number_format, ctx.getString(R.string.contact_number_loading))
                fetchContact(req.customerID) { finalContact ->
                    (ctx as? android.app.Activity)?.runOnUiThread {
                        tvDetailContact.text = ctx.getString(R.string.contact_number_format, finalContact)
                    } ?: run {
                        tvDetailContact.text = ctx.getString(R.string.contact_number_format, finalContact)
                    }
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

            // Close button
            btnClose.setOnClickListener { dialog.dismiss() }

            // Show Message button in dialog only when Waiting for customer to claim (status 7)
            if (currentStatus == 7) {
                btnMsg.visibility = View.VISIBLE
                btnMsg.setOnClickListener {
                    val context = dlgView.context
                    val myId = SharedPrefManager.getUserId(context)
                    val otherId = when {
                        myId == null -> req.customerID
                        myId == req.ownerID -> req.customerID
                        else -> req.customerID
                    }
                    val otherName = req.customerName
                    val activity = context as? androidx.fragment.app.FragmentActivity
                    if (activity != null) {
                        try {
                            val chatFrag = com.example.capstone2.customer.ChatFragment.newInstance(otherId, null, otherName)
                            dialog.dismiss()
                            activity.supportFragmentManager.beginTransaction()
                                .replace(R.id.flFragment, chatFrag)
                                .addToBackStack(null)
                                .commit()
                        } catch (e: Exception) {
                            dialog.dismiss()
                            Toast.makeText(context, "Unable to open chat: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        dialog.dismiss()
                        Toast.makeText(context, "Cannot open chat from this context", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                btnMsg.visibility = View.GONE
            }

            // NEW: Mark as Complete button (visible unless already Completed or Rejected)
            val showMarkComplete = currentStatus !in setOf(8, 9, 13)
            if (showMarkComplete) {
                btnMarkComplete.visibility = View.VISIBLE
                btnMarkComplete.setOnClickListener {
                    // Confirm and update to Completed (8)
                    try {
                        androidx.appcompat.app.AlertDialog.Builder(ctx)
                            .setTitle(R.string.mark_as_complete)
                            .setMessage(R.string.confirm_mark_complete_message)
                            .setPositiveButton(R.string.confirm) { d, _ ->
                                d.dismiss()
                                dialog.dismiss()
                                onButtonClick(req, 8)
                            }
                            .setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
                            .show()
                    } catch (_: Exception) {
                        // Fallback without dialog
                        dialog.dismiss()
                        onButtonClick(req, 8)
                    }
                }
            } else {
                btnMarkComplete.visibility = View.GONE
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
            7 -> "Waiting for customer to claim"
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
