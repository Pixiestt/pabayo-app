package com.example.capstone2.adapter

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.SharedPrefManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import com.example.capstone2.util.StatusColorProvider
import androidx.core.graphics.drawable.DrawableCompat

class CustomerTrackAdapter(
    private var requests: List<Request>,
    private val onMarkCompleteClick: (Request) -> Unit, // Callback for mark complete button
    private val onEditRequest: (Request) -> Unit // Callback when user wants to edit a request
) : RecyclerView.Adapter<CustomerTrackAdapter.CustomerTrackViewHolder>() {

    inner class CustomerTrackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val requestId: TextView = itemView.findViewById(R.id.tvRequestID)
        val dateRequested: TextView = itemView.findViewById(R.id.tvDateRequested)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val tvPaymentAmount: TextView? = itemView.findViewById(R.id.tvPaymentAmount)
        val btnMarkComplete: Button = itemView.findViewById(R.id.btnMarkComplete)
        val btnMore: Button = itemView.findViewById(R.id.btnMore)
    }

    // Helper to parse formatted currency strings like "₱1,234.50" safely
    private fun parseAmountStringSafe(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.replace(Regex("[^0-9.,-]"), "").replace(",", "")
        return cleaned.toDoubleOrNull()
    }

    // Map status IDs to readable names based on the database status table
    private fun statusTextOf(statusId: Int): String = when (statusId) {
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

    private fun isoNow(): String {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", java.util.Locale.getDefault())
        fmt.timeZone = java.util.TimeZone.getDefault()
        return fmt.format(java.util.Date())
    }

    // --- Time helpers using java.time ---
    private val sysZone: java.time.ZoneId get() = java.time.ZoneId.systemDefault()

    private fun parseFlexibleToZdt(raw: String?, assumeUtcIfNaive: Boolean): java.time.ZonedDateTime? {
        if (raw.isNullOrBlank()) return null
        val t = raw.trim()
        // 1) Try ISO with explicit offset or Z
        try {
            val odt = java.time.OffsetDateTime.parse(t, java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            return odt.atZoneSameInstant(sysZone)
        } catch (_: Exception) { }
        // 2) Try local date-time with optional fraction
        try {
            val normalized = if (t.contains('T')) t else t.replace(' ', 'T')
            val builder = java.time.format.DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .appendPattern("HH:mm:ss")
                .optionalStart()
                .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 1, 9, true)
                .optionalEnd()
                .toFormatter()
            val ldt = java.time.LocalDateTime.parse(normalized, builder)
            return if (assumeUtcIfNaive) ldt.atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(sysZone)
            else ldt.atZone(sysZone)
        } catch (_: Exception) { }
        // 3) Fallback plain pattern
        try {
            val f = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val ldt = java.time.LocalDateTime.parse(t.replace('T', ' '), f)
            return if (assumeUtcIfNaive) ldt.atZone(java.time.ZoneOffset.UTC).withZoneSameInstant(sysZone)
            else ldt.atZone(sysZone)
        } catch (_: Exception) { }
        return null
    }

    private fun canonicalIso(raw: String?, assumeUtcIfNaive: Boolean): String {
        val zdt = parseFlexibleToZdt(raw, assumeUtcIfNaive)
        return if (zdt != null) zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")) else (raw ?: "")
    }

    private fun formatFriendly(raw: String?, assumeUtcIfNaive: Boolean): String? {
        val zdt = parseFlexibleToZdt(raw, assumeUtcIfNaive) ?: return raw
        val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")
        return zdt.format(fmt)
    }
    // --- end time helpers ---

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerTrackViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_customer_track, parent, false)
        return CustomerTrackViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerTrackViewHolder, position: Int) {
        val request = requests[position]
        val ctx = holder.itemView.context

        holder.requestId.text = ctx.getString(R.string.request_id_format, request.requestID)
        holder.dateRequested.text = ctx.getString(
            R.string.date_requested_format,
            request.submittedAt ?: ctx.getString(R.string.not_available)
        )

        val statusText = statusTextOf(request.statusID)
        holder.status.text = ctx.getString(R.string.status_format, statusText)

        // Apply centralized status color
        try {
            val colorInt = StatusColorProvider.getColorFor(request.statusID)
            holder.status.setTextColor(colorInt)
        } catch (_: Exception) { /* ignore */ }

        // NEW: show payment amount on customer row if available (robust fallbacks)
        holder.tvPaymentAmount?.let { tv ->
            val amt = request.paymentAmount
                ?: request.payment?.amount
                ?: parseAmountStringSafe(request.payment?.amountString)
            tv.visibility = View.VISIBLE
            if (amt != null && amt >= 0.0) {
                tv.text = ctx.getString(R.string.payment_amount_format, amt)
            } else {
                tv.text = ctx.getString(R.string.payment_amount_not_set)
            }
        }

        // Persist status transition in local timeline whenever list binds
        try {
            // Ensure initial seed exists
            val existing = SharedPrefManager.getRequestStatusHistory(ctx, request.requestID)
            if (existing.isEmpty()) {
                val seedAt = request.submittedAt ?: request.dateUpdated ?: isoNow()
                val seedIso = if (request.submittedAt != null) canonicalIso(seedAt, assumeUtcIfNaive = false)
                               else canonicalIso(seedAt, assumeUtcIfNaive = true)
                SharedPrefManager.appendStatusIfChanged(ctx, request.requestID, 1, statusTextOf(1), seedIso)
            }
            // Append current status with its timestamp if changed (server times are UTC when naive)
            SharedPrefManager.appendStatusIfChanged(
                ctx,
                request.requestID,
                request.statusID,
                statusText,
                canonicalIso(request.dateUpdated ?: isoNow(), assumeUtcIfNaive = true)
            )
        } catch (_: Exception) { /* ignore */ }

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
        // Persist a local timeline of status changes.
        // For each request, append an entry if the status has changed compared to the last stored one.
        // Since we don't have a direct Context here, we'll append later when opening details.
        // However, we can still filter the list now.

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
        val btnViewProfile: Button? = dialog.findViewById(R.id.btnViewProfile)
        btnViewProfile?.visibility = View.GONE
        val tvDetailContact: TextView? = dialog.findViewById(R.id.tvDetailContact)
        val tvStatusUpdatedAt: TextView? = dialog.findViewById(R.id.tvStatusUpdatedAt)
        val tvTimelineHeader: TextView? = dialog.findViewById(R.id.tvTimelineHeader)
        val llTimelineContainer: LinearLayout? = dialog.findViewById(R.id.llTimelineContainer)
        val tvDetailPaymentAmount: TextView? = dialog.findViewById(R.id.tvDetailPaymentAmount)

        // Use formatted string resources to populate the dialog
        tvDetailCustomerName.text = context.getString(R.string.customer_format, request.customerName)
        tvDetailSackQty.text = context.getString(R.string.sacks_format, request.sackQuantity)
        tvDetailServices.text = context.getString(R.string.services_format, request.serviceName)
        tvDetailSchedule.text = context.getString(R.string.schedule_format, request.schedule ?: context.getString(R.string.not_set))

        // Show payment amount in dialog if present (robust fallbacks)
        tvDetailPaymentAmount?.let { tv ->
            val amt = request.paymentAmount
                ?: request.payment?.amount
                ?: parseAmountStringSafe(request.payment?.amountString)
            if (amt != null && amt >= 0.0) {
                tv.visibility = View.VISIBLE
                tv.text = context.getString(R.string.payment_amount_format, amt)
            } else {
                tv.visibility = View.VISIBLE
                tv.text = context.getString(R.string.payment_amount_not_set)
            }
        }

        // Show pickup location if available or if service requires it
        if (!request.pickupLocation.isNullOrEmpty()) {
            tvDetailPickupLocation.text = context.getString(R.string.pickup_location_format, request.pickupLocation)
            tvDetailPickupLocation.visibility = View.VISIBLE
        } else {
            when (request.serviceID) {
                1L, 2L, 5L, 6L -> {
                    tvDetailPickupLocation.text = context.getString(
                        R.string.pickup_location_format,
                        context.getString(R.string.not_set)
                    )
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
            when (request.serviceID) {
                1L, 3L, 5L, 7L -> {
                    tvDetailDeliveryLocation.text = context.getString(
                        R.string.delivery_location_format,
                        context.getString(R.string.not_set)
                    )
                    tvDetailDeliveryLocation.visibility = View.VISIBLE
                }
                else -> tvDetailDeliveryLocation.visibility = View.GONE
            }
        }

        tvDetailComment.text = context.getString(R.string.comment_format, request.comment ?: context.getString(R.string.not_available))
        tvDetailSubmittedAt.text = context.getString(R.string.submitted_at_format, request.submittedAt ?: context.getString(R.string.not_available))

        // Contact handling: show contact row when available (Call/Copy removed)
        fun showContact(contact: String?) {
            if (tvDetailContact == null) return
            if (contact.isNullOrBlank()) {
                tvDetailContact.visibility = View.GONE
            } else {
                tvDetailContact.visibility = View.VISIBLE
                tvDetailContact.text = context.getString(R.string.contact_number_format, contact)
            }
        }

        // Helper to format timestamps into a friendly string; falls back to raw when parsing fails
        fun formatUpdatedAt(raw: String?): String? {
            return formatFriendly(raw, assumeUtcIfNaive = false)
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
            6 -> 85   // Rider out for delivery
            7 -> 95   // Waiting for customer to claim
            13 -> 100 // Delivered
            8 -> 100  // Completed
            9 -> 0    // Rejected
            else -> 0
        }

        // Explicitly show progress UI for customers
        tvDetailProgressLabel.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        // Defensive: ensure determinate horizontal progress and max
        try {
            progressBar.isIndeterminate = false
        } catch (_: Exception) { }
        try {
            progressBar.max = 100
        } catch (_: Exception) { }

        // Tint the progress bar according to centralized status color
        try {
            val colorInt = StatusColorProvider.getColorFor(request.statusID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                progressBar.progressTintList = ColorStateList.valueOf(colorInt)
                try { progressBar.progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#DDDDDD")) } catch (_: Exception) {}
            } else {
                try {
                    // Wrap the drawable and apply a tint list which works better across many pre-Lollipop drawables
                    val original = progressBar.progressDrawable
                    val wrapped = DrawableCompat.wrap(original).mutate()
                    DrawableCompat.setTintList(wrapped, ColorStateList.valueOf(colorInt))
                    // Also tint the secondary/progress background layers if present
                    try {
                        if (wrapped is android.graphics.drawable.LayerDrawable) {
                            val sec = wrapped.findDrawableByLayerId(android.R.id.secondaryProgress)
                            sec?.setColorFilter(Color.parseColor("#DDDDDD"), PorterDuff.Mode.SRC_IN)
                        }
                    } catch (_: Exception) { }
                    progressBar.progressDrawable = wrapped
                } catch (_: Exception) {
                    // Fallback: color filter on the drawable
                    try {
                        val pd = progressBar.progressDrawable
                        pd.setColorFilter(colorInt, PorterDuff.Mode.SRC_IN)
                        if (pd is android.graphics.drawable.LayerDrawable) {
                            val progLayer = pd.findDrawableByLayerId(android.R.id.progress)
                            progLayer?.setColorFilter(colorInt, PorterDuff.Mode.SRC_IN)
                        }
                    } catch (_: Exception) { }
                }
            }
            tvDetailProgressLabel.setTextColor(colorInt)
        } catch (_: Exception) { /* ignore color issues */ }

        // Debug log the computed progress and status so we can trace UI problems at runtime
        try {
            Log.d("CustomerTrackAdapter", "Request=${request.requestID} status=${request.statusID} computedProgress=$progress")
        } catch (_: Exception) { }

        // Ensure progress assignment happens on the UI thread and invalidate to force redraw
        try {
            if (progressBar.isLaidOut) {
                progressBar.progress = progress
                progressBar.invalidate()
                progressBar.refreshDrawableState()
            } else {
                progressBar.post {
                    progressBar.progress = progress
                    progressBar.invalidate()
                    progressBar.refreshDrawableState()
                }
            }
        } catch (_: Exception) {
            // Fallback direct set
            try {
                progressBar.progress = progress
                progressBar.invalidate()
                progressBar.refreshDrawableState()
            } catch (_: Exception) { }
        }
        tvDetailProgressLabel.text = context.getString(R.string.progress_format, progress)

        // --- Build and show status timeline ---
        try {
            val currentStatusText = statusTextOf(request.statusID)
            // Seed timeline with submittedAt if empty; then append current status if changed.
            val existing = SharedPrefManager.getRequestStatusHistory(context, request.requestID)
            if (existing.isEmpty()) {
                val seedAt = request.submittedAt ?: request.dateUpdated ?: isoNow()
                val seedIso = if (request.submittedAt != null) canonicalIso(seedAt, assumeUtcIfNaive = false)
                               else canonicalIso(seedAt, assumeUtcIfNaive = true)
                SharedPrefManager.appendStatusIfChanged(
                    context,
                    request.requestID,
                    1, // initial status assumed as Subject for approval
                    statusTextOf(1),
                    seedIso
                )
            }
            // Append current status and get the up-to-date history list
            val changedAt = request.dateUpdated ?: isoNow()
            var history = SharedPrefManager.appendStatusIfChanged(
                context,
                request.requestID,
                request.statusID,
                currentStatusText,
                canonicalIso(changedAt, assumeUtcIfNaive = true)
            )

            // On-the-fly migrate any naive timestamps in history by interpreting them as UTC for display only
            val displayHistory = history.map { entry ->
                val at = entry.at
                if (!at.contains("+") && !at.contains("Z")) entry.copy(at = canonicalIso(at, assumeUtcIfNaive = true)) else entry
            }

            // NEW: Backfill Processing (5) just for display if 12 exists but 5 is missing
            val adjustedHistory = run {
                val has12 = displayHistory.any { it.statusID == 12 }
                val has5 = displayHistory.any { it.statusID == 5 }
                if (has12 && !has5) {
                    val list = displayHistory.toMutableList()
                    // Find the first 12 entry (latest by time will be sorted later)
                    val twelve = list.firstOrNull { it.statusID == 12 }
                    if (twelve != null) {
                        val twelveMs = try { parseFlexibleToZdt(twelve.at, assumeUtcIfNaive = true)?.toInstant()?.toEpochMilli() } catch (_: Exception) { null }
                        // Choose a time slightly before 12 and after the most recent prior entry if possible
                        var candidate = if (twelveMs != null) twelveMs - 500 else null
                        val priorMax = list
                            .filter { it !== twelve }
                            .mapNotNull { h -> try { parseFlexibleToZdt(h.at, assumeUtcIfNaive = true)?.toInstant()?.toEpochMilli() } catch (_: Exception) { null } }
                            .filter { ms -> candidate == null || ms < (twelveMs ?: Long.MAX_VALUE) }
                            .maxOrNull()
                        if (priorMax != null && candidate != null && candidate <= priorMax) {
                            candidate = priorMax + 1
                        }
                        val iso = try {
                            if (candidate != null) java.time.Instant.ofEpochMilli(candidate)
                                .atZone(sysZone)
                                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"))
                            else canonicalIso(twelve.at, assumeUtcIfNaive = true)
                        } catch (_: Exception) {
                            canonicalIso(twelve.at, assumeUtcIfNaive = true)
                        }
                        list.add(SharedPrefManager.StatusChangeEntry(5, "Processing", iso))
                    }
                    list
                } else displayHistory
            }

            var latestInstant: java.time.Instant? = null

            if (adjustedHistory.isNotEmpty() && llTimelineContainer != null && tvTimelineHeader != null) {
                tvTimelineHeader.visibility = View.VISIBLE
                llTimelineContainer.visibility = View.VISIBLE
                llTimelineContainer.removeAllViews()
                // Sort by actual timestamp (latest first)
                val sorted = adjustedHistory.sortedByDescending { h ->
                    try { parseFlexibleToZdt(h.at, assumeUtcIfNaive = true)?.toInstant() } catch (_: Exception) { null }
                }
                // Track latest instant from history
                latestInstant = sorted.firstOrNull()?.let {
                    try { parseFlexibleToZdt(it.at, assumeUtcIfNaive = true)?.toInstant() } catch (_: Exception) { null }
                }
                for (entry in sorted) {
                    val tv = TextView(context)
                    val whenText = formatFriendly(entry.at, assumeUtcIfNaive = true) ?: entry.at
                    tv.text = context.getString(R.string.status_timeline_entry, whenText, entry.statusText)
                    tv.textSize = 12f
                    tv.setTextColor(context.resources.getColor(R.color.black, null))
                    tv.setPadding(0, 4, 0, 4)
                    llTimelineContainer.addView(tv)
                }
            } else {
                tvTimelineHeader?.visibility = View.GONE
                llTimelineContainer?.visibility = View.GONE
            }

            // Now compute and display the Status updated label using the freshest time between request.dateUpdated and timeline
            val dateUpdatedInstant = try { parseFlexibleToZdt(request.dateUpdated, assumeUtcIfNaive = true)?.toInstant() } catch (_: Exception) { null }
            val latest = listOfNotNull(dateUpdatedInstant, latestInstant).maxOrNull()
            if (latest == null) {
                tvStatusUpdatedAt?.visibility = View.GONE
            } else {
                tvStatusUpdatedAt?.visibility = View.VISIBLE
                val friendly = java.time.ZonedDateTime.ofInstant(latest, sysZone).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                tvStatusUpdatedAt?.text = context.getString(R.string.status_updated_at, friendly)
            }
        } catch (_: Exception) {
            tvTimelineHeader?.visibility = View.GONE
            llTimelineContainer?.visibility = View.GONE
            // Fall back display for status updated using request.dateUpdated
            val friendlyUpdated = formatFriendly(request.dateUpdated, assumeUtcIfNaive = true)
            if (friendlyUpdated.isNullOrBlank()) {
                tvStatusUpdatedAt?.visibility = View.GONE
            } else {
                tvStatusUpdatedAt?.visibility = View.VISIBLE
                tvStatusUpdatedAt?.text = context.getString(R.string.status_updated_at, friendlyUpdated)
            }
        }

        dialog.show()
    }
}
