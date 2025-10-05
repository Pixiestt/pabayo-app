package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Conversation

class ConversationsAdapter(private val onClick: (Conversation) -> Unit) : ListAdapter<Conversation, ConversationsAdapter.VH>(Diff()) {

    // Ensure adapter never shows duplicate partner rows: dedupe by partnerID preserving order
    override fun submitList(list: List<Conversation>?) {
        if (list == null) {
            super.submitList(null)
            return
        }
        val seen = mutableSetOf<Long>()
        val deduped = mutableListOf<Conversation>()
        for (c in list) {
            if (seen.add(c.partnerID)) deduped.add(c)
        }
        super.submitList(deduped)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_conversation, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(itemView: View, val onClick: (Conversation) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val tvPartner: TextView? = itemView.findViewById(R.id.tvPartner)
        private val tvLast: TextView? = itemView.findViewById(R.id.tvLastMessage)
        private val tvTime: TextView? = itemView.findViewById(R.id.tvTime)
        private val tvUnread: TextView? = itemView.findViewById(R.id.tvUnread)

        fun bind(c: Conversation) {
            // Normal partner display
            tvPartner?.text = c.partnerName ?: "User ${c.partnerID}"
            // Preview text should be just the last message
            tvLast?.text = c.lastMessage ?: ""

            // Try to parse common timestamp formats and format to a short human-friendly form
            var displayTime = c.lastMessageAt ?: ""
            try {
                val s = c.lastMessageAt
                if (!s.isNullOrBlank()) {
                    try {
                        val fmtIn = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val ldt = java.time.LocalDateTime.parse(s, fmtIn)
                        val fmtOut = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                        displayTime = ldt.format(fmtOut)
                    } catch (_: Exception) {
                        // fallback: try ISO
                        try {
                            val instant = java.time.Instant.parse(s)
                            val zdt = instant.atZone(java.time.ZoneId.systemDefault())
                            val fmtOut2 = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                            displayTime = zdt.format(fmtOut2)
                        } catch (_: Exception) {
                            // leave raw
                            displayTime = s
                        }
                    }
                }
            } catch (_: Exception) {
                displayTime = c.lastMessageAt ?: ""
            }

            tvTime?.text = displayTime

            // By default make preview text readable/darker; when unread show a stronger emphasis
            tvLast?.setTextColor("#212121".toColorInt())
            if (c.unreadCount > 0) {
                tvUnread?.visibility = View.VISIBLE
                tvUnread?.text = c.unreadCount.toString()
                // emphasize preview text when there are unread messages
                tvLast?.setTextColor("#000000".toColorInt())
            } else {
                tvUnread?.visibility = View.GONE
            }

            itemView.setOnClickListener { onClick(c) }
        }
    }

    class Diff : DiffUtil.ItemCallback<Conversation>() {
        override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation): Boolean = oldItem.conversationID == newItem.conversationID
        override fun areContentsTheSame(oldItem: Conversation, newItem: Conversation): Boolean = oldItem == newItem
    }
}
