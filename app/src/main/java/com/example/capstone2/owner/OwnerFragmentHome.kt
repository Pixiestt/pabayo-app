package com.example.capstone2.owner

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.capstone2.R
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.OwnerRequestViewModel
import com.example.capstone2.viewmodel.OwnerRequestViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import com.example.capstone2.data.models.Conversation

class OwnerFragmentHome : Fragment(R.layout.owner_fragment_home) {
    
    private lateinit var btnViewRequests: Button
    private lateinit var btnViewMessages: Button
    private lateinit var btnUpdateStatus: Button
    private lateinit var btnViewHistory: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private var tvRequestsBadge: TextView? = null
    private var tvMessagesBadge: TextView? = null

    // ViewModel to fetch owner requests count
    private var ownerRequestViewModel: OwnerRequestViewModel? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Hide bottom navigation bar
        bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView)
        bottomNavigationView.visibility = View.GONE
        
        // Find button views
        btnViewMessages = view.findViewById(R.id.btnViewMessages)
        btnViewRequests = view.findViewById(R.id.btnViewRequests)
        btnUpdateStatus = view.findViewById(R.id.btnUpdateStatus)
        btnViewHistory = view.findViewById(R.id.btnViewHistory)
        tvRequestsBadge = view.findViewById(R.id.tvRequestsBadge)
        tvMessagesBadge = view.findViewById(R.id.tvMessagesBadge)

        // Immediately show last known unread count (persisted) so UI doesn't flash empty
        try {
            val saved = SharedPrefManager.getUnreadMessagesCount(requireContext())
            updateMessagesBadge(saved)
        } catch (_: Exception) { }

        // Navigate to Messages when pressed
        btnViewMessages.setOnClickListener {
            // Ask the activity to show the MessagesFragment
            (activity as? OwnerMainActivity)?.openMessages()
        }

        // Set button click listeners
        btnViewRequests.setOnClickListener {
            navigateToRequests()
        }
        
        btnUpdateStatus.setOnClickListener {
            navigateToTrack()
        }
        
        btnViewHistory.setOnClickListener {
            navigateToHistory()
        }

        // Setup ViewModel to observe requests and update badge
        setupRequestsBadge()
        // Setup unread messages badge
        setupMessagesBadge()
    }

    override fun onResume() {
        super.onResume()
        // Refresh counts every time we come back to Home
        ownerRequestViewModel?.fetchOwnerRequests()
        // Refresh conversations to update unread badge
        fetchUnreadMessagesCount()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure bottom navigation becomes visible when leaving this fragment
        bottomNavigationView.visibility = View.VISIBLE
        tvRequestsBadge = null
        tvMessagesBadge = null
    }
    
    private fun navigateToRequests() {
        // Change to Request tab
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.request
    }
    
    private fun navigateToTrack() {
        // Change to Track tab
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.selectedItemId = R.id.track
    }
    
    private fun navigateToHistory() {
        // Request the activity to open the history fragment directly since history is not in bottom nav
        (activity as? OwnerMainActivity)?.openHistory()
    }

    private fun setupRequestsBadge() {
        val token = SharedPrefManager.getAuthToken(requireContext()) ?: return
        val authedApi = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApi)
        val factory = OwnerRequestViewModelFactory(repository, requireActivity().application)
        ownerRequestViewModel = ViewModelProvider(this, factory)[OwnerRequestViewModel::class.java]

        ownerRequestViewModel?.ownerRequests?.observe(viewLifecycleOwner) { list ->
            // Count requests with statusID == 1 (Subject for approval)
            val count = (list ?: emptyList()).count { it.statusID == 1 }
            updateRequestsBadge(count)
        }

        // initial fetch
        ownerRequestViewModel?.fetchOwnerRequests()
    }

    private fun updateRequestsBadge(count: Int) {
        val badge = tvRequestsBadge ?: return
        if (count <= 0) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            val display = if (count > 99) "99+" else count.toString()
            badge.text = display
        }
    }

    private fun setupMessagesBadge() {
        // initial load
        fetchUnreadMessagesCount()
    }

    private fun fetchUnreadMessagesCount() {
        val token = SharedPrefManager.getAuthToken(requireContext()) ?: return
        val api = ApiClient.getApiService { token }
        lifecycleScope.launch {
            try {
                val resp = api.getConversations()
                if (!resp.isSuccessful) {
                    // propagate zero to activity and local UI
                    (activity as? OwnerMainActivity)?.setMessagesUnreadCount(0)
                    updateMessagesBadge(0)
                    return@launch
                }
                val bodyStr = try { resp.body()?.string() } catch (_: Exception) { null }
                if (bodyStr.isNullOrBlank()) {
                    (activity as? OwnerMainActivity)?.setMessagesUnreadCount(0)
                    updateMessagesBadge(0)
                    return@launch
                }
                val gson = Gson()
                val je = JsonParser.parseString(bodyStr)
                // Find the array of conversations under common keys or top-level array
                fun findArray(elem: JsonElement): JsonElement? {
                    if (elem.isJsonArray) return elem
                    if (elem.isJsonObject) {
                        val obj = elem.asJsonObject
                        val keys = listOf("conversations", "data", "items", "results")
                        for (k in keys) {
                            if (obj.has(k)) {
                                val child = obj.get(k)
                                if (child.isJsonArray) return child
                            }
                        }
                        val it = obj.entrySet().iterator()
                        while (it.hasNext()) {
                            val entry = it.next()
                            val found = findArray(entry.value)
                            if (found != null) return found
                        }
                    }
                    return null
                }
                val arr = findArray(je)
                val listType = object : TypeToken<List<Conversation>>() {}.type
                val conversations: List<Conversation> = if (arr != null) gson.fromJson(arr, listType) else gson.fromJson(bodyStr, listType)
                val totalUnread = conversations.sumOf { it.unreadCount.coerceAtLeast(0) }

                // Persist + propagate to activity (which also updates badge if present)
                (activity as? OwnerMainActivity)?.setMessagesUnreadCount(totalUnread)
                // Update local badge directly as well
                updateMessagesBadge(totalUnread)
            } catch (_: Exception) {
                (activity as? OwnerMainActivity)?.setMessagesUnreadCount(0)
                updateMessagesBadge(0)
            }
        }
    }

    private fun updateMessagesBadge(count: Int) {
        val badge = tvMessagesBadge ?: return
        if (count <= 0) {
            badge.visibility = View.GONE
        } else {
            badge.visibility = View.VISIBLE
            val display = if (count > 99) "99+" else count.toString()
            badge.text = display
        }
    }
}