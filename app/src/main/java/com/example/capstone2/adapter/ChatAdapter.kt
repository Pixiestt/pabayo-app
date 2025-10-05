// new file
package com.example.capstone2.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Message

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

class ChatAdapter(private val currentUserId: Long) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

    // Helper: pick a deterministic background for a message (cycles through variants)
    private fun pickBackgroundRes(m: Message, isSent: Boolean): Int {
        val sentVariants = intArrayOf(
            R.drawable.bg_message_sent_variant_1,
            R.drawable.bg_message_sent_variant_2,
            R.drawable.bg_message_sent_variant_3,
            R.drawable.bg_message_sent_variant_4
        )
        val recvVariants = intArrayOf(
            R.drawable.bg_message_received_variant_1,
            R.drawable.bg_message_received_variant_2,
            R.drawable.bg_message_received_variant_3,
            R.drawable.bg_message_received_variant_1 // reuse first as a simple 4th fallback
        )
        val variants = if (isSent) sentVariants else recvVariants
        val key = m.id?.hashCode() ?: (m.message + ":" + m.senderID).hashCode()
        val idx = kotlin.math.abs(key) % variants.size
        return variants[idx]
    }

    override fun getItemViewType(position: Int): Int {
        val msg = getItem(position)
        return if (msg.senderID == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val v = inflater.inflate(R.layout.item_message_sent, parent, false)
            SentViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.item_message_received, parent, false)
            ReceivedViewHolder(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        if (holder is SentViewHolder) holder.bind(msg, currentUserId)
        if (holder is ReceivedViewHolder) holder.bind(msg, currentUserId)
    }

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: View? = itemView.findViewById(R.id.messageBubbleSent)
        private val tvSenderName: TextView? = itemView.findViewById(R.id.tvSenderNameSent)
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageSent)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeSent)
        fun bind(m: Message, currentUserId: Long) {
            // Prefer server-provided senderName; if absent and the message is from the current user show "You"
            val name = m.senderName ?: m.receiverName ?: if (m.senderID == currentUserId) "You" else "User ${m.senderID}"
            tvSenderName?.text = name

            // Apply background variant and adjust text color for contrast
            try {
                val adapter = this@SentViewHolder
                // We'll compute the background by using the adapter's pick function via itemView.context resources
                val ctx = itemView.context
                val key = m.id?.hashCode() ?: (m.message + ":" + m.senderID).hashCode()
                val variants = listOf(
                    R.drawable.bg_message_sent_variant_1,
                    R.drawable.bg_message_sent_variant_2,
                    R.drawable.bg_message_sent_variant_3,
                    R.drawable.bg_message_sent_variant_4
                )
                val idx = kotlin.math.abs(key) % variants.size
                bubble?.setBackgroundResource(variants[idx])

                // Sent bubbles are generally darker – use white text for readability
                tvMessage?.setTextColor(Color.WHITE)
                tvSenderName?.setTextColor(Color.WHITE)
                tvTime?.setTextColor(Color.parseColor("#DDDDDD"))
            } catch (_: Exception) {
                // ignore background failures
            }

            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message
            tvTime?.text = m.timestamp ?: ""
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: View? = itemView.findViewById(R.id.messageBubbleReceived)
        private val tvSenderName: TextView? = itemView.findViewById(R.id.tvSenderName)
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeReceived)
        fun bind(m: Message, currentUserId: Long) {
            val name = m.senderName ?: m.receiverName ?: if (m.senderID == currentUserId) "You" else "User ${m.senderID}"
            tvSenderName?.text = name

            try {
                val key = m.id?.hashCode() ?: (m.message + ":" + m.senderID).hashCode()
                val variants = listOf(
                    R.drawable.bg_message_received_variant_1,
                    R.drawable.bg_message_received_variant_2,
                    R.drawable.bg_message_received_variant_3,
                    R.drawable.bg_message_received_variant_1
                )
                val idx = kotlin.math.abs(key) % variants.size
                bubble?.setBackgroundResource(variants[idx])

                // Received bubbles are light -> use dark text
                tvMessage?.setTextColor(Color.BLACK)
                tvSenderName?.setTextColor(Color.parseColor("#333333"))
                tvTime?.setTextColor(Color.parseColor("#666666"))
            } catch (_: Exception) {
                // ignore
            }

            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message
            tvTime?.text = m.timestamp ?: ""
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            // Prefer server-assigned id when available
            val oldId = oldItem.id
            val newId = newItem.id
            if (oldId != null && newId != null) return oldId == newId
            // Fallback: use a composite of senderID + timestamp + message content
            val oldKey = "${oldItem.senderID}:${oldItem.timestamp ?: ""}:${oldItem.message}"
            val newKey = "${newItem.senderID}:${newItem.timestamp ?: ""}:${newItem.message}"
            return oldKey == newKey
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
