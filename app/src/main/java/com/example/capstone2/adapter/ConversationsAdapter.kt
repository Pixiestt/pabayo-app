package com.example.capstone2.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.data.models.Conversation

class ConversationsAdapter(private val onClick: (Conversation) -> Unit) : ListAdapter<Conversation, ConversationsAdapter.VH>(Diff()) {

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
            tvPartner?.text = c.partnerName ?: "User ${c.partnerID}"
            tvLast?.text = c.lastMessage ?: ""
            tvTime?.text = c.lastMessageAt ?: ""
            if (c.unreadCount > 0) {
                tvUnread?.visibility = View.VISIBLE
                tvUnread?.text = c.unreadCount.toString()
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

