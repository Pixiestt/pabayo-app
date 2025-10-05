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
            adapter.submitList(enriched)
            rvMessages.visibility = View.VISIBLE
            try { rvMessages.bringToFront() } catch (_: Exception) {}
            rvMessages.post { if (enriched.isNotEmpty()) try { rvMessages.scrollToPosition(enriched.size - 1) } catch (_: Exception) {} }
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
