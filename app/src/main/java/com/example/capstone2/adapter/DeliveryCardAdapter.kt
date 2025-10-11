package com.example.capstone2.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
        val tvStatusLabel: TextView = itemView.findViewById(R.id.tvStatusLabel)
        val btnInitiate: Button = itemView.findViewById(R.id.btnInitiate)
        val btnDone: Button = itemView.findViewById(R.id.btnDone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_delivery_card, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req = items[position]
        holder.tvTitle.text = holder.itemView.context.getString(R.string.request_id_format, req.requestID)
        holder.tvSubtitle.text = holder.itemView.context.getString(R.string.customer_format, req.customerName)

        val ongoingText = if (mode == DeliveryMode.PICKUPS) "Pickup ongoing" else "Ongoing delivery"
        val doneText = if (mode == DeliveryMode.PICKUPS) "Pickup done" else "Delivery done"
        val isDone = req.statusID == 8

        holder.tvStatusLabel.text = when {
            isDone -> doneText
            else -> req.submittedAt?.let { holder.itemView.context.getString(R.string.date_requested_format, it) } ?: "Pending"
        }

        // Configure buttons depending on mode
        if (mode == DeliveryMode.PICKUPS) {
            val canInitiate = req.statusID == 10
            val isOngoing = req.statusID == 2

            holder.btnInitiate.isEnabled = canInitiate
            holder.btnDone.isEnabled = isOngoing

            holder.btnInitiate.setOnClickListener {
                // Update status to "delivery boy pickup" (2)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        requestRepository.updateRequestStatus(req.requestID, 2)
                    } catch (e: Exception) {
                        Log.e("DeliveryCardAdapter", "Failed to update pickup status", e)
                    }
                }
                holder.tvStatusLabel.text = ongoingText
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = true
                onAction(req, DeliveryAction.INITIATE)
            }

            holder.btnDone.setOnClickListener {
                onAction(req, DeliveryAction.DONE)
                holder.tvStatusLabel.text = doneText
                holder.btnInitiate.isEnabled = false
                holder.btnDone.isEnabled = false

                // Add this coroutine to update backend status to 4 (pending)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        requestRepository.updateRequestStatus(req.requestID, 4)
                    } catch (e: Exception) {
                        Log.e("DeliveryCardAdapter", "Failed to update pickup done status", e)
                    }
                }
            }

        } else { // DELIVERIES
            holder.btnInitiate.text = "Initiate Delivery"
            holder.btnDone.text = "Delivery Done"


            val statusText = when (req.statusID) {
                12 -> "Milling done – ready for delivery"
                6 -> "Ongoing delivery"
                13 -> "Delivery completed"
                else -> "Pending"
            }
            holder.tvStatusLabel.text = statusText

            // Only enable "Initiate Delivery" if statusID = 12 (Milling done)
            holder.btnInitiate.isEnabled = req.statusID == 12
            // "Delivery Done" is only enabled if statusID = 6 (Rider out for delivery)
            holder.btnDone.isEnabled = req.statusID == 6

            holder.btnInitiate.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = requestRepository.updateRequestStatus(req.requestID, 6) // 6 = Rider out for delivery
                        if (response.isSuccessful) {
                            req.statusID = 6
                            CoroutineScope(Dispatchers.Main).launch {
                                holder.tvStatusLabel.text = "Ongoing delivery"
                                holder.btnInitiate.isEnabled = false
                                holder.btnDone.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DeliveryCardAdapter", "Error initiating delivery", e)
                    }
                }
                onAction(req, DeliveryAction.INITIATE)
            }

            holder.btnDone.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = requestRepository.updateRequestStatus(req.requestID, 13) // 13 = Delivered
                        if (response.isSuccessful) {
                            req.statusID = 13
                            CoroutineScope(Dispatchers.Main).launch {
                                holder.tvStatusLabel.text = "Delivery done"
                                holder.btnInitiate.isEnabled = false
                                holder.btnDone.isEnabled = false
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DeliveryCardAdapter", "Error marking delivery done", e)
                    }
                }
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