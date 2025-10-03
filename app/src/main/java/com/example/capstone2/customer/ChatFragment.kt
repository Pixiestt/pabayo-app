package com.example.capstone2.customer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
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

    private var otherUserId: Long = -1L
    private var conversationID: String? = null
    private var currentUserId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // expecting arguments: otherUserID (Long) and optional conversationID (String)
            if (it.containsKey("otherUserID")) {
                otherUserId = it.getLong("otherUserID", -1L)
            }
            if (it.containsKey("conversationID")) {
                conversationID = it.getString("conversationID")
            }
        }

        // read current user id from shared prefs (string stored by login flow)
        val prefs = requireContext().getSharedPreferences("capstone_prefs", Context.MODE_PRIVATE)
        val uidString = prefs.getString("userID", null)
        currentUserId = try { uidString?.toLong() ?: -1L } catch (e: Exception) { -1L }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_chat, container, false)
        rvMessages = v.findViewById(R.id.rvMessages)
        etMessage = v.findViewById(R.id.etMessage)
        btnSend = v.findViewById(R.id.btnSend)

        adapter = ChatAdapter(currentUserId)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        val factory = ChatViewModelFactory(requireContext(), currentUserId)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            // scroll to bottom
            if (list.isNotEmpty()) {
                rvMessages.scrollToPosition(list.size - 1)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        // initial load
        if (conversationID != null) {
            viewModel.loadConversation(conversationID = conversationID)
        } else if (otherUserId != -1L) {
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
        fun newInstance(otherUserID: Long, conversationID: String? = null) = ChatFragment().apply {
            arguments = Bundle().apply {
                putLong("otherUserID", otherUserID)
                conversationID?.let { putString("conversationID", it) }
            }
        }
    }
}

