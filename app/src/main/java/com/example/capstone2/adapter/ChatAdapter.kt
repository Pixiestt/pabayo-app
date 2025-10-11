// new file
package com.example.capstone2.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Message

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

class ChatAdapter(private val currentUserId: Long) : ListAdapter<Message, RecyclerView.ViewHolder>(DiffCallback()) {

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
        @Suppress("UNUSED_PARAMETER")
        fun bind(m: Message, _currentUserId: Long) {
            // Determine display name
            val name = m.senderName ?: if (m.senderID == currentUserId) "You" else "User ${m.senderID}"
            tvSenderName?.text = name

            // Message text and time
            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message
            tvTime?.text = m.timestamp ?: ""

            // Style sent bubble (prefer drawable but fallback to color)
            try {
                bubble?.setBackgroundResource(R.drawable.bg_message_sent)
            } catch (_: Exception) {
                bubble?.setBackgroundColor("#DFF0D8".toColorInt()) // light green fallback
            }

            // Colors
            tvMessage?.setTextColor(Color.BLACK)
            tvSenderName?.setTextColor(Color.BLACK)
            tvTime?.setTextColor("#666666".toColorInt())
        }
    }

        @Suppress("UNUSED_PARAMETER")
        fun bind(m: Message, _currentUserId: Long) {
        private val tvSenderName: TextView? = itemView.findViewById(R.id.tvSenderNameReceived)
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeReceived)
            // Use a single white bubble for received messages
        fun bind(m: Message, currentUserId: Long) {
                bubble?.setBackgroundResource(R.drawable.bg_message_received)
            val name = m.senderName ?: m.receiverName ?: if (m.senderID == currentUserId) "You" else "User ${m.senderID}"
            tvSenderName?.text = name

            // Message text and time
            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message
            tvTime?.text = m.timestamp ?: ""

            // Choose a received bubble variant deterministically
            try {
                val key = m.id?.hashCode() ?: (m.message + ":" + m.senderID).hashCode()
                val variants = listOf(
                    R.drawable.bg_message_received_variant_1,
                    R.drawable.bg_message_received_variant_2,
                    R.drawable.bg_message_received_variant_3
                )
                val idx = kotlin.math.abs(key) % variants.size
                bubble?.setBackgroundResource(variants[idx])
            } catch (_: Exception) {
                // fallback to white background if drawables are missing
                bubble?.setBackgroundColor(Color.WHITE)
            }

            // Colors for received messages
            tvMessage?.setTextColor(Color.BLACK)
            tvSenderName?.setTextColor("#333333".toColorInt())
            tvTime?.setTextColor("#666666".toColorInt())
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
