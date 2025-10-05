package com.example.capstone2.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.ChatAdapter
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.ChatViewModel
import com.example.capstone2.viewmodel.ChatViewModelFactory
import android.util.Log

class ChatFragment : Fragment() {
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private lateinit var tvChatTitle: TextView

    private var otherUserId: Long = -1L
    private var conversationID: String? = null
    private var currentUserId: Long = -1L
    private var otherName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            if (it.containsKey("otherUserID")) otherUserId = it.getLong("otherUserID", -1L)
            if (it.containsKey("conversationID")) conversationID = it.getString("conversationID")
            if (it.containsKey("otherName")) otherName = it.getString("otherName")
        }

        currentUserId = SharedPrefManager.getUserId(requireContext()) ?: -1L
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_chat, container, false)
        tvChatTitle = v.findViewById(R.id.tvChatTitle)
        rvMessages = v.findViewById(R.id.rvMessages)
        etMessage = v.findViewById(R.id.etMessage)
        btnSend = v.findViewById(R.id.btnSend)

        otherName?.let { tvChatTitle.text = it; tvChatTitle.visibility = View.VISIBLE }

        adapter = ChatAdapter(currentUserId)
        rvMessages.setHasFixedSize(true)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
        rvMessages.isNestedScrollingEnabled = true
        rvMessages.clipToPadding = false
        try { rvMessages.elevation = 8f * resources.displayMetrics.density } catch (_: Exception) {}

        // Adjust RecyclerView bottom padding so last message is visible above the input (post to ensure measurements are ready)
        try {
            v.post {
                try {
                    val input = v.findViewById<View?>(R.id.layoutInput)
                    val inputH = input?.height ?: (56 * resources.displayMetrics.density).toInt()
                    val extra = (8 * resources.displayMetrics.density).toInt()
                    rvMessages.setPadding(rvMessages.paddingLeft, rvMessages.paddingTop, rvMessages.paddingRight, inputH + extra)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        val factory = ChatViewModelFactory(requireContext(), currentUserId)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            val snapshot = list?.toList() ?: emptyList()
            val enriched = snapshot.map { msg ->
                if (msg.senderName != null) return@map msg
                val inferredName = when (msg.senderID) {
                    currentUserId -> SharedPrefManager.getUserFullName(requireContext()) ?: "You"
                    otherUserId -> otherName
                    else -> null
                }
                if (inferredName != null) msg.copy(senderName = inferredName) else msg
            }
            // Robust ordering: prefer sorting by parsed timestamps when there are multiple parseable times.
            fun parseTimeToMillis(t: String?): Long? {
                if (t == null) return null
                try { val asLong = t.toLongOrNull(); if (asLong != null) return if (asLong < 1000000000000L) asLong * 1000L else asLong } catch (_: Exception) {}
                try { return java.time.Instant.parse(t).toEpochMilli() } catch (_: Exception) {}
                try {
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    val ldt = java.time.LocalDateTime.parse(t, fmt)
                    return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (_: Exception) {}
                return null
            }

            val parsedList = enriched.map { it to try { parseTimeToMillis(it.timestamp) } catch (_: Exception) { null } }
            val parsedCount = parsedList.count { it.second != null }
            val sorted = when {
                parsedCount >= 2 -> {
                    // Sort by time ascending (oldest first)
                    parsedList.sortedBy { it.second ?: Long.MIN_VALUE }.map { it.first }
                }
                else -> {
                    // Not enough parseable times: apply heuristics on raw timestamp strings.
                    val rawTimestamps = enriched.mapNotNull { it.timestamp }
                    fun isLikelyIso(ts: String?): Boolean {
                        if (ts == null) return false
                        return ts.length >= 16 && ts[4] == '-' && ts[7] == '-' && ts[10] == ' '
                    }

                    // If first and last raw timestamps look like the common yyyy-MM-dd HH:mm:ss format, compare lexicographically.
                    val firstRaw = enriched.firstOrNull()?.timestamp
                    val lastRaw = enriched.lastOrNull()?.timestamp
                    val lexReverse = if (firstRaw != null && lastRaw != null && isLikelyIso(firstRaw) && isLikelyIso(lastRaw)) {
                        firstRaw > lastRaw
                    } else {
                        // Fallback heuristic: sample pairs across the list and count decreasing pairs (newest-first). If majority decreasing, reverse.
                        var dec = 0; var inc = 0
                        val sample = rawTimestamps.take(6)
                        for (i in 0 until sample.size - 1) {
                            val a = sample[i]; val b = sample[i+1]
                            if (a > b) dec++ else if (a < b) inc++
                        }
                        dec > inc && dec > 0
                    }

                    if (lexReverse) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Heuristic: reversing list because timestamps appear newest-first")
                        enriched.reversed()
                    } else enriched
                }
            }
            adapter.submitList(sorted)
            // Broadcast the newest message preview so the Conversations list can update its preview if needed
             try {
                 val lastMsg = sorted.lastOrNull()
                 if (lastMsg != null) {
                     // Determine partner id in case fragment was opened by conversationID (otherUserId may be -1)
                     val partnerIdToSave = try {
                         if (otherUserId != -1L) otherUserId
                         else {
                             val sid = lastMsg.senderID ?: -1L
                             val rid = lastMsg.receiverID ?: -1L
                             if (sid == currentUserId) rid else sid
                         }
                     } catch (_: Exception) { otherUserId }

                     // Persist preview for offline/slow-server situations
                     try {
                         com.example.capstone2.repository.SharedPrefManager.saveConversationPreview(requireContext(), partnerIdToSave, lastMsg.conversationID, lastMsg.message, lastMsg.timestamp)
                     } catch (_: Exception) {}

                     val itPreview = android.content.Intent("com.example.capstone2.NEW_MESSAGE_PREVIEW")
                     itPreview.putExtra("partnerID", partnerIdToSave)
                     itPreview.putExtra("conversationID", lastMsg.conversationID)
                     itPreview.putExtra("lastMessage", lastMsg.message)
                     itPreview.putExtra("lastMessageAt", lastMsg.timestamp)
                     requireActivity().sendBroadcast(itPreview)
                 }
             } catch (_: Exception) {}
             rvMessages.visibility = View.VISIBLE
            try { rvMessages.bringToFront() } catch (_: Exception) {}
            rvMessages.post { if (sorted.isNotEmpty()) try { rvMessages.scrollToPosition(sorted.size - 1) } catch (_: Exception) {} }
        }

        viewModel.error.observe(viewLifecycleOwner) { err -> err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() } }

        if (conversationID != null) {
            Toast.makeText(requireContext(), "Loading conversation...", Toast.LENGTH_SHORT).show()
            viewModel.loadConversation(conversationID = conversationID)
        } else if (otherUserId != -1L) {
            Toast.makeText(requireContext(), "Loading conversation...", Toast.LENGTH_SHORT).show()
            viewModel.loadConversation(otherUserID = otherUserId)
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (otherUserId == -1L) {
                Toast.makeText(requireContext(), "No recipient specified", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(receiverID = otherUserId, text = text, conversationID = conversationID)
            etMessage.setText("")
        }

        return v
    }

    companion object {
        private const val TAG = "ChatFragment"
        @JvmStatic
        fun newInstance(otherUserID: Long, conversationID: String? = null, otherName: String? = null) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("otherUserID", otherUserID)
                conversationID?.let { putString("conversationID", it) }
                otherName?.let { putString("otherName", it) }
            }
        }
    }
}
