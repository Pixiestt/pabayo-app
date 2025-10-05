package com.example.capstone2.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SwitchCompat
import com.example.capstone2.R
import com.example.capstone2.adapter.ChatAdapter
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.ChatViewModel
import com.example.capstone2.viewmodel.ChatViewModelFactory
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomnavigation.BottomNavigationView

class ChatFragment : Fragment() {

    private lateinit var rvMessages: RecyclerView
    private var etMessage: EditText? = null
    private var btnSend: ImageButton? = null
    private lateinit var adapter: ChatAdapter
    private lateinit var viewModel: ChatViewModel
    private lateinit var tvChatTitle: TextView

    // Debug panel views
    private var debugPanel: LinearLayout? = null
    private var scrollDebug: ScrollView? = null
    private var tvDebug: TextView? = null
    private var btnCopyDebug: Button? = null
    private var switchDebugToggle: SwitchCompat? = null

    // Force debug panel visible by default (set to false to restore original behaviour)
    // Deprecated: preference-controlled now. Default kept false.
    private val FORCE_DEBUG_ALWAYS_ON = false
    // When true show a modal AlertDialog with debug output so it's impossible to miss during testing
    private val SHOW_DEBUG_DIALOG = false

    private var otherUserId: Long = -1L
    private var conversationID: String? = null
    private var currentUserId: Long = -1L
    private var otherName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            // expecting arguments: otherUserID (Long) and optional conversationID (String) and otherName
            if (it.containsKey("otherUserID")) {
                otherUserId = it.getLong("otherUserID", -1L)
            }
            if (it.containsKey("conversationID")) {
                conversationID = it.getString("conversationID")
            }
            if (it.containsKey("otherName")) {
                otherName = it.getString("otherName")
            }
        }

        // Use centralized SharedPrefManager to read current user id
        currentUserId = SharedPrefManager.getUserId(requireContext()) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_chat, container, false)
        tvChatTitle = v.findViewById(R.id.tvChatTitle)
        rvMessages = v.findViewById(R.id.rvMessages)
        // The fragment no longer contains the input controls; prefer fragment-local views, else fall back to the activity's persistent input bar
        val fragEt = v.findViewById<EditText?>(R.id.etMessage)
        val fragBtn = v.findViewById<ImageButton?>(R.id.btnSend)
        var layoutInputView: LinearLayout? = v.findViewById(R.id.layoutInput)
        if (fragEt != null && fragBtn != null) {
            etMessage = fragEt
            btnSend = fragBtn
        } else {
            // fallback to activity-scoped input controls that we added to the activity layout
            try {
                etMessage = requireActivity().findViewById(R.id.etMessage)
            } catch (_: Exception) {
                // keep a non-null placeholder to avoid later NPEs; we'll guard when using it
            }
            try {
                btnSend = requireActivity().findViewById(R.id.btnSend)
            } catch (_: Exception) {
            }
            try { layoutInputView = requireActivity().findViewById(R.id.layoutInputActivity) } catch (_: Exception) { layoutInputView = null }
        }

        // Debug panel views
        debugPanel = v.findViewById(R.id.debugPanel)
        scrollDebug = v.findViewById(R.id.scrollDebug)
        tvDebug = v.findViewById(R.id.tvDebug)
        btnCopyDebug = v.findViewById(R.id.btnCopyDebug)
        switchDebugToggle = v.findViewById(R.id.switchDebugToggle)

        // Initialize switch from preference and set panel visibility accordingly
        try {
            val enabled = SharedPrefManager.isDebugPanelEnabled(requireContext()) || FORCE_DEBUG_ALWAYS_ON
            switchDebugToggle?.isChecked = enabled
            debugPanel?.visibility = if (enabled) View.VISIBLE else View.GONE
        } catch (_: Exception) {
            // fallback to hidden by default
            debugPanel?.visibility = View.GONE
            switchDebugToggle?.isChecked = false
        }

        // Set placeholder if needed
        if (tvDebug?.text.isNullOrBlank()) tvDebug?.text = getString(R.string.waiting_debug)

        // Wire the switch listener to persist preference and update panel visibility
        switchDebugToggle?.setOnCheckedChangeListener { _, isChecked ->
            try {
                SharedPrefManager.setDebugPanelEnabled(requireContext(), isChecked)
                debugPanel?.visibility = if (isChecked) View.VISIBLE else View.GONE
            } catch (_: Exception) {}
        }

        // Show the other party's name when provided so the UI clearly indicates the opened conversation
        otherName?.let {
            tvChatTitle.text = it
            tvChatTitle.visibility = View.VISIBLE
        }

        // Quick runtime toast to verify ids used by this fragment
        try {
            Toast.makeText(requireContext(), "currentUserId=$currentUserId otherUserId=$otherUserId", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {}

        // RecyclerView setup
        adapter = ChatAdapter(currentUserId)
        rvMessages.setHasFixedSize(true)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        rvMessages.adapter = adapter
        // Ensure RecyclerView is above other UI elements (debug panel, backgrounds) so messages are visible
        rvMessages.bringToFront()
        rvMessages.isNestedScrollingEnabled = true
        rvMessages.clipToPadding = false
        // Ensure RecyclerView is visually elevated above any overlapping UI (px = dp * density)
        try { rvMessages.elevation = 8f * resources.displayMetrics.density } catch (_: Exception) {}

        // Programmatically ensure input bar is anchored to fragment bottom and adjust RecyclerView padding so items are not hidden.
        try {
            // Keep the input visible and above the messages; rely on layout (RelativeLayout alignParentBottom) and a safe RecyclerView bottom padding
            layoutInputView?.visibility = View.VISIBLE
            layoutInputView?.bringToFront()
            // Safe padding (approx input + bottom navigation) so last item won't be obscured
            val bottomPaddingDp = 88 // adjust if your bottom nav or input size differs
            val padPx = (bottomPaddingDp * resources.displayMetrics.density).toInt()
            rvMessages.setPadding(rvMessages.paddingLeft, rvMessages.paddingTop, rvMessages.paddingRight, padPx)
            rvMessages.bringToFront()
        } catch (_: Exception) {}

        val factory = ChatViewModelFactory(requireContext(), currentUserId)
        viewModel = ViewModelProvider(this, factory).get(ChatViewModel::class.java)

        viewModel.messages.observe(viewLifecycleOwner) { list ->
            Log.d(TAG, "messages observer invoked: size=${list?.size ?: 0}")
            // provide an immutable snapshot to the adapter to ensure DiffUtil can detect changes
            val snapshot = list?.toList() ?: emptyList()

            // Ensure messages have readable sender names when possible.
            // - If the API provided senderName, keep it.
            // - If senderID == currentUserId, mark as "You" (or leave server name if present).
            // - If senderID matches otherUserId and `otherName` was passed to the fragment, use that.
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

            // Ensure RecyclerView visible
            rvMessages.visibility = View.VISIBLE
            // Bring to front again after the list update in case other views were re-laid
            try { rvMessages.bringToFront() } catch (_: Exception) {}

            // Show messages content in debug panel (concise) to make them obvious during troubleshooting
            val joined = enriched.joinToString(separator = "\n") { m -> "[${m.senderID} -> ${m.receiverID}] ${m.message}" }
            if (joined.isNotBlank()) {
                // Append safely using resources and TextView.append to avoid concatenation/setText lint warnings
                tvDebug?.append("\n")
                tvDebug?.append(getString(R.string.messages_label))
                tvDebug?.append(joined)
                scrollDebug?.post { scrollDebug?.fullScroll(View.FOCUS_DOWN) }
            }

            // Use ListAdapter's diffing (avoid notifyDataSetChanged). Keep scrolling to bottom after submitList so new items are visible.
            rvMessages.post {
                // ListAdapter will handle updating the items efficiently; just scroll to the last item when present.
                if (enriched.isNotEmpty()) try { rvMessages.scrollToPosition(enriched.size - 1) } catch (_: Exception) {}
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            err?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show() }
        }

        // Observe debug LiveData and show debug panel when content is present
        // Modified: when FORCE_DEBUG_ALWAYS_ON is true we never hide the panel; we still update text.
        viewModel.debug.observe(viewLifecycleOwner) { dbg ->
            Log.d(TAG, "debug observer invoked; length=${'$'}{dbg?.length ?: 0}")
            val text = dbg ?: ""
            // Use persisted preference (fall back to FORCE_DEBUG_ALWAYS_ON)
            val showPanel = try { SharedPrefManager.isDebugPanelEnabled(requireContext()) || FORCE_DEBUG_ALWAYS_ON } catch (_: Exception) { false }
            // Only show panel if the user enabled it (or FORCE_DEBUG_ALWAYS_ON). When disabled, keep panel hidden
            // but still update the internal tvDebug text so the logs are preserved for when the user enables the panel.
            val prev = tvDebug?.text?.toString() ?: ""
            val combined = if (prev.isBlank() || prev == getString(R.string.waiting_debug)) text else "$prev\n$text"
            tvDebug?.text = combined
            debugPanel?.visibility = if (showPanel) View.VISIBLE else View.GONE

            // Bring debug panel to front and refresh so it's visible immediately
            debugPanel?.bringToFront()
            debugPanel?.requestLayout()
            debugPanel?.invalidate()

            // Scroll debug ScrollView to bottom so the latest debug content is visible
            scrollDebug?.post { scrollDebug?.fullScroll(View.FOCUS_DOWN) }

            // Optionally show a modal dialog with debug content to guarantee visibility
            if (SHOW_DEBUG_DIALOG && text.isNotBlank()) {
                try {
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Debug Output")
                        .setMessage(text.take(4000))
                        .setPositiveButton("Close", null)
                        .show()
                } catch (_: Exception) {}
            }

            // Also show a Toast for immediate feedback (short-lived)
            if (!text.isBlank()) {
                try { Toast.makeText(requireContext(), text.take(200), Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
            }
        }

        // Copy debug text to clipboard
        btnCopyDebug?.setOnClickListener {
            val txt = tvDebug?.text?.toString() ?: ""
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("chat-debug", txt)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Debug copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        // initial load
        if (conversationID != null) {
            tvDebug?.text = getString(R.string.requesting_conversation_id, conversationID ?: "", currentUserId)
            Toast.makeText(requireContext(), "Loading conversation...", Toast.LENGTH_SHORT).show()
            viewModel.loadConversation(conversationID = conversationID)
        } else if (otherUserId != -1L) {
            tvDebug?.text = getString(R.string.requesting_conversation_other, otherUserId, currentUserId)
            Toast.makeText(requireContext(), "Loading conversation...", Toast.LENGTH_SHORT).show()
            viewModel.loadConversation(otherUserID = otherUserId)
        }

        btnSend?.setOnClickListener {
            val text = etMessage?.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) return@setOnClickListener
            if (otherUserId == -1L) {
                Toast.makeText(requireContext(), "No recipient specified", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.sendMessage(receiverID = otherUserId, text = text, conversationID = conversationID)
            try { etMessage?.setText("") } catch (_: Exception) {}
        }

        return v
    }

    override fun onResume() {
        super.onResume()
        try {
            // Ensure activity-level input (if present) is visible and above bottom nav
            val activityInput = try { requireActivity().findViewById<LinearLayout?>(R.id.layoutInputActivity) } catch (_: Exception) { null }
            val activityEt = try { requireActivity().findViewById<EditText?>(R.id.etMessage) } catch (_: Exception) { null }
            val activityBtn = try { requireActivity().findViewById<ImageButton?>(R.id.btnSend) } catch (_: Exception) { null }
            val bottomNav = try { requireActivity().findViewById<View?>(R.id.bottomNavigationView) } catch (_: Exception) { null }

            activityInput?.let { ai ->
                ai.visibility = View.VISIBLE
                ai.bringToFront()
                try { ai.translationZ = 2000f } catch (_: Exception) {}
                try { ai.elevation = 24f * resources.displayMetrics.density } catch (_: Exception) {}
                // if input already measured, adjust rv padding; else post a runnable
                val adjustPadding = {
                    try {
                        val inputH = ai.height.takeIf { it > 0 } ?: (56 * resources.displayMetrics.density).toInt()
                        val navH = bottomNav?.height ?: (75 * resources.displayMetrics.density).toInt()
                        val extra = (8 * resources.displayMetrics.density).toInt()
                        rvMessages.setPadding(rvMessages.paddingLeft, rvMessages.paddingTop, rvMessages.paddingRight, inputH + navH + extra)
                        rvMessages.bringToFront()
                    } catch (_: Exception) {}
                }
                if (ai.height > 0) adjustPadding() else ai.post { adjustPadding() }
                // Dynamically set constraint: if bottomNav visible anchor top of nav, else anchor to parent bottom
                try {
                    val lp = ai.layoutParams
                    if (lp is ConstraintLayout.LayoutParams) {
                        if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
                            lp.bottomToTop = bottomNav.id
                            lp.bottomToBottom = ConstraintLayout.LayoutParams.UNSET
                        } else {
                            lp.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            lp.bottomToTop = ConstraintLayout.LayoutParams.UNSET
                        }
                        ai.layoutParams = lp
                    }
                } catch (_: Exception) {}
                try { Toast.makeText(requireContext(), "inputFound=true", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
            }
            if (activityInput == null) try { Toast.makeText(requireContext(), "inputFound=false", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}

            activityEt?.let { it.visibility = View.VISIBLE }
            activityBtn?.let { it.visibility = View.VISIBLE }
            bottomNav?.let { try { it.translationZ = 0f; it.elevation = 0f } catch (_: Exception) {} }
        } catch (_: Exception) {}
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu, inflater: android.view.MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_chat_debug, menu)
        // Initialize menu title based on current preference
        try {
            val enabled = SharedPrefManager.isDebugPanelEnabled(requireContext()) || FORCE_DEBUG_ALWAYS_ON
            val item = menu.findItem(R.id.action_toggle_debug)
            item?.title = if (enabled) getString(R.string.hide_debug) else getString(R.string.show_debug)
        } catch (_: Exception) {}
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_debug -> {
                try {
                    val current = SharedPrefManager.isDebugPanelEnabled(requireContext())
                    val next = !current
                    SharedPrefManager.setDebugPanelEnabled(requireContext(), next)
                    debugPanel?.visibility = if (next) View.VISIBLE else View.GONE
                    item.title = if (next) getString(R.string.hide_debug) else getString(R.string.show_debug)
                    // Also update the switch if present
                    switchDebugToggle?.isChecked = next
                } catch (_: Exception) {}
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
