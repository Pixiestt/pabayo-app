package com.example.capstone2.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Request
import com.example.capstone2.repository.RequestRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class DeliveryMode { PICKUPS, DELIVERIES }
enum class DeliveryAction { INITIATE, DONE }

class DeliveryCardAdapter(
    private var items: MutableList<Request>,
    private val mode: DeliveryMode,
    private val onAction: (Request, DeliveryAction) -> Unit,
    private val requestRepository: RequestRepository
) : RecyclerView.Adapter<DeliveryCardAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        val tvSacks: TextView = itemView.findViewById(R.id.tvSacks)
        val tvStatusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
        val btnInitiate: Button = itemView.findViewById(R.id.btnInitiate)
        val btnDone: Button = itemView.findViewById(R.id.btnDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ctx = holder.itemView.context
        val req = items[position]
        holder.tvTitle.text = ctx.getString(R.string.request_id_format, req.requestID)
        holder.tvSubtitle.text = ctx.getString(R.string.customer_format, req.customerName)
        // New: show sacks count
        holder.tvSacks.text = ctx.getString(R.string.sacks_format, req.sackQuantity)

        // New: show a basic details dialog on card tap
        holder.itemView.setOnClickListener {
            val details = buildString {
                appendLine(ctx.getString(R.string.request_id_format, req.requestID))
                appendLine(ctx.getString(R.string.customer_format, req.customerName))
                appendLine(ctx.getString(R.string.sacks_format, req.sackQuantity))
                appendLine(ctx.getString(R.string.service_format, req.serviceName))
                req.pickupLocation?.takeIf { it.isNotBlank() }?.let {
                    appendLine(ctx.getString(R.string.pickup_location_format, it))
                }
                req.deliveryLocation?.takeIf { it.isNotBlank() }?.let {
                    appendLine(ctx.getString(R.string.delivery_location_format, it))
                }
                req.schedule?.takeIf { it.isNotBlank() }?.let {
                    appendLine(ctx.getString(R.string.schedule_format, it))
                }
            }
            AlertDialog.Builder(ctx)
                .setTitle(R.string.request_details_title)
                .setMessage(details.trim())
                .setPositiveButton(R.string.close, null)
                .show()
        }

        val ongoingText = if (mode == DeliveryMode.PICKUPS) ctx.getString(R.string.status_pickup_ongoing) else ctx.getString(R.string.status_ongoing_delivery)
        val doneText = if (mode == DeliveryMode.PICKUPS) ctx.getString(R.string.status_pickup_done) else ctx.getString(R.string.status_delivery_done)
        val pendingText = ctx.getString(R.string.pending_label)
        val isDone = req.statusID == 8

        holder.tvStatusLabel.text = when {
            isDone -> doneText
            else -> req.submittedAt?.let { ctx.getString(R.string.date_requested_format, it) } ?: pendingText
        }

        // Configure buttons depending on mode
        if (mode == DeliveryMode.PICKUPS) {
            val canInitiate = req.statusID == 10 || req.statusID == 11
            val isOngoing = req.statusID == 2

            holder.btnInitiate.isEnabled = canInitiate
            holder.btnDone.isEnabled = isOngoing

            holder.btnInitiate.setOnClickListener {
                // Optimistic UI updates; actual network handled by fragment via callback
                holder.tvStatusLabel.text = ongoingText
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = true
                onAction(req, DeliveryAction.INITIATE)
            }

            holder.btnDone.setOnClickListener {
                // Optimistic UI updates; actual network handled by fragment via callback
                holder.tvStatusLabel.text = doneText
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = false
                onAction(req, DeliveryAction.DONE)
            }

        } else { // DELIVERIES
            holder.btnInitiate.text = ctx.getString(R.string.action_initiate_delivery)
            holder.btnDone.text = ctx.getString(R.string.action_delivery_done)

            val statusText = when (req.statusID) {
                12 -> ctx.getString(R.string.status_milling_ready_delivery)
                6 -> ctx.getString(R.string.status_ongoing_delivery)
                13 -> ctx.getString(R.string.status_delivery_done)
                else -> pendingText
            }
            holder.tvStatusLabel.text = statusText

            // Only enable "Initiate Delivery" if statusID = 12 (Milling done)
            holder.btnInitiate.isEnabled = req.statusID == 12
            // "Delivery Done" is only enabled if statusID = 6 (Rider out for delivery)
            holder.btnDone.isEnabled = req.statusID == 6

            holder.btnInitiate.setOnClickListener {
                // Optimistic UI; network handled by fragment
                holder.tvStatusLabel.text = ctx.getString(R.string.status_ongoing_delivery)
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = true
                onAction(req, DeliveryAction.INITIATE)
            }

            holder.btnDone.setOnClickListener {
                // Optimistic UI; network handled by fragment
                holder.tvStatusLabel.text = ctx.getString(R.string.status_delivery_done)
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = false
                onAction(req, DeliveryAction.DONE)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun submit(newItems: List<Request>) {
        items = newItems.toMutableList()
        notifyDataSetChanged()
    }
}