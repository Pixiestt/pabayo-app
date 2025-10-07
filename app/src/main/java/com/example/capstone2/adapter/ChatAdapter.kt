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
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageSent)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeSent)

        fun bind(m: Message, currentUserId: Long) {
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
            tvTime?.setTextColor("#666666".toColorInt())
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: View? = itemView.findViewById(R.id.messageBubbleReceived)
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeReceived)

        fun bind(m: Message, currentUserId: Long) {
            // Message text and time
            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message
            tvTime?.text = m.timestamp ?: ""

            // Use a single white bubble for received messages
            try {
                bubble?.setBackgroundResource(R.drawable.bg_message_received)
            } catch (_: Exception) {
                bubble?.setBackgroundColor(Color.WHITE)
            }

            // Colors for received messages
            tvMessage?.setTextColor(Color.BLACK)
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
