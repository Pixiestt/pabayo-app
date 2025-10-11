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
            // sender name intentionally not shown here — title already displays the conversation name

            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message ?: ""
            tvTime?.text = m.timestamp ?: ""

            try {
                bubble?.setBackgroundResource(R.drawable.bg_message_sent)
            } catch (_: Exception) {
                try {
                    bubble?.setBackgroundColor("#DFF0D8".toColorInt())
                } catch (_: Exception) {
                    bubble?.setBackgroundColor(Color.parseColor("#DFF0D8"))
                }
            }

            tvMessage?.setTextColor(Color.BLACK)
            try { tvTime?.setTextColor("#666666".toColorInt()) } catch (_: Exception) { tvTime?.setTextColor(Color.DKGRAY) }
        }
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bubble: View? = itemView.findViewById(R.id.messageBubbleReceived)
        private val tvMessage: TextView? = itemView.findViewById(R.id.tvMessageReceived)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTimeReceived)

        fun bind(m: Message, currentUserId: Long) {
            // sender name intentionally not shown here — title already displays the conversation name

            tvMessage?.visibility = View.VISIBLE
            tvMessage?.text = m.message ?: ""
            tvTime?.text = m.timestamp ?: ""

            try {
                // Pick a deterministic variant if drawables exist
                val key = m.id?.hashCode() ?: (m.message ?: "").hashCode() xor m.senderID.hashCode()
                val variants = listOf(
                    R.drawable.bg_message_received_variant_1,
                    R.drawable.bg_message_received_variant_2,
                    R.drawable.bg_message_received_variant_3
                )
                val idx = kotlin.math.abs(key) % variants.size
                bubble?.setBackgroundResource(variants[idx])
            } catch (_: Exception) {
                try {
                    bubble?.setBackgroundResource(R.drawable.bg_message_received)
                } catch (_: Exception) {
                    bubble?.setBackgroundColor(Color.WHITE)
                }
            }

            tvMessage?.setTextColor(Color.BLACK)
            try { tvTime?.setTextColor("#666666".toColorInt()) } catch (_: Exception) { tvTime?.setTextColor(Color.DKGRAY) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            val oldId = oldItem.id
            val newId = newItem.id
            if (oldId != null && newId != null) return oldId == newId
            val oldKey = "${oldItem.senderID}:${oldItem.timestamp ?: ""}:${oldItem.message ?: ""}"
            val newKey = "${newItem.senderID}:${newItem.timestamp ?: ""}:${newItem.message ?: ""}"
            return oldKey == newKey
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}
