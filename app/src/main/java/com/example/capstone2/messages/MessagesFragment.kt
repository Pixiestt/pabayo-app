package com.example.capstone2.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.ConversationsAdapter
import com.example.capstone2.data.models.Conversation
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.customer.ChatFragment
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import android.util.Log
import java.io.File
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.ScrollView
import android.content.pm.ApplicationInfo
import android.content.BroadcastReceiver
import android.content.IntentFilter

class MessagesFragment : Fragment() {

    private var rv: RecyclerView? = null
    private var tvEmpty: TextView? = null
    private lateinit var adapter: ConversationsAdapter
    private var previewReceiver: BroadcastReceiver? = null

    companion object {
        private const val TAG = "MessagesFragment"
    }

    // Runtime check for whether the app is debuggable (avoids referencing BuildConfig)
    private fun isDebug(): Boolean {
        return try {
            (requireContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) { false }
    }

    // Reusable timestamp parser for this fragment
    private fun parseTimeToMillis(t: String?): Long? {
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_messages_list, container, false)
        rv = v.findViewById(R.id.rvConversations)
        tvEmpty = v.findViewById(R.id.tvEmpty)

        adapter = ConversationsAdapter { conv ->
            // Open chat fragment for selected conversation using otherUserID (server will compute canonical conversationID)
            val chat = ChatFragment.newInstance(conv.partnerID, null, conv.partnerName)
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.flFragment, chat)
                .addToBackStack(null)
                .commit()
        }

        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // Register receiver to update conversation preview when chat receives/sends a new message
        try {
            previewReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    try {
                        if (intent == null) return
                        val partnerId = intent.getLongExtra("partnerID", -1L)
                        if (partnerId == -1L) return
                        val convId = intent.getStringExtra("conversationID")
                        val lastMsg = intent.getStringExtra("lastMessage")
                        var lastAt = intent.getStringExtra("lastMessageAt")
                        if (lastAt.isNullOrBlank()) {
                            try {
                                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                lastAt = java.time.LocalDateTime.now().format(fmt)
                            } catch (_: Exception) { lastAt = null }
                        }
                        // Update adapter list: replace or insert, then sort newest-first
                        val current = adapter.currentList.toMutableList()
                        var replaced = false
                        for (i in current.indices) {
                            if (current[i].partnerID == partnerId) {
                                val c = current[i]
                                val updated = c.copy(conversationID = convId ?: c.conversationID, lastMessage = lastMsg ?: c.lastMessage, lastMessageAt = lastAt ?: c.lastMessageAt)
                                current[i] = updated
                                replaced = true
                                break
                            }
                        }
                        if (!replaced) {
                            // create a minimal conversation entry when missing
                            val newConv = Conversation(conversationID = convId ?: "", partnerID = partnerId, partnerName = null, lastMessage = lastMsg ?: "", lastMessageAt = lastAt, unreadCount = 0)
                            current.add(newConv)
                        }
                        // Sort newest-first by parsed timestamp
                        try {
                            val sorted = current.sortedWith(compareByDescending<Conversation> { c -> parseTimeToMillis(c.lastMessageAt) ?: Long.MIN_VALUE })
                            adapter.submitList(sorted)
                        } catch (_: Exception) {
                            adapter.submitList(current)
                        }
                    } catch (_: Exception) { }
                }
            }
            requireContext().registerReceiver(previewReceiver, IntentFilter("com.example.capstone2.NEW_MESSAGE_PREVIEW"))
        } catch (_: Exception) { }

        loadConversations()

        return v
    }

    private fun loadConversations() {
        lifecycleScope.launch {
            try {
                val api = ApiClient.getApiService { SharedPrefManager.getAuthToken(requireContext()) }
                val resp = api.getConversations()
                if (resp.isSuccessful) {
                    val bodyStr = try { resp.body()?.string() } catch (_: Exception) { null }

                    // Save raw response to external app-specific storage so you can pull it without adb
                    try {
                        val outDir = requireContext().getExternalFilesDir(null)
                        if (outDir != null && !bodyStr.isNullOrBlank()) {
                            val outFile = File(outDir, "conversations_raw_${System.currentTimeMillis()}.json")
                            outFile.writeText(bodyStr)
                            // (debug) disabled: previously saved raw conversations JSON to external files for debugging
                            // Keeping try/catch structure omitted to avoid changing behavior; no file writes performed now.
                        }
                    } catch (e: Exception) {
                        if (Log.isLoggable(TAG, Log.WARN)) Log.w(TAG, "failed to write conversations file", e)
                    }
                    // debug logging removed
                    if (bodyStr.isNullOrBlank()) {
                        this@MessagesFragment.showEmpty()
                        return@launch
                    }
                    try {
                        val gson = Gson()
                        val je: JsonElement = JsonParser.parseString(bodyStr)

                        // try find array under common keys
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
                                // search properties for array
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

                        // Enrich conversations with inferred partner names when missing
                        val enriched = mutableListOf<Conversation>()
                        // Determine a JsonArray to inspect (prefer `arr` if found)
                        val jsonArr = when {
                            arr != null && arr.isJsonArray -> arr.asJsonArray
                            je.isJsonArray -> je.asJsonArray
                            else -> null
                        }

                        // Helper: extract latest message text and timestamp from a JsonElement conversation candidate
                        fun extractLatestMessage(elem: JsonElement?): Pair<String?, String?> {
                            if (elem == null) return Pair(null, null)
                            try {
                                if (!elem.isJsonObject) return Pair(null, null)
                                val obj = elem.asJsonObject

                                // Quick string fields that often contain preview
                                val quickKeys = listOf("lastMessage", "last_message", "latestMessage", "latest_message", "preview", "last")
                                val timeKeysSibling = listOf("lastMessageAt", "last_message_at", "lastAt", "last_at", "updatedAt", "updated_at", "lastMessageTime")
                                for (k in quickKeys) {
                                    if (obj.has(k) && !obj.get(k).isJsonNull) {
                                        try {
                                            val s = obj.get(k).asString
                                            if (!s.isNullOrBlank()) {
                                                // Try to find a sibling timestamp in the same object
                                                var siblingTime: String? = null
                                                for (tk in timeKeysSibling) {
                                                    if (obj.has(tk) && !obj.get(tk).isJsonNull) {
                                                        try { siblingTime = obj.get(tk).asString; break } catch (_: Exception) { }
                                                    }
                                                }
                                                return Pair(s, siblingTime)
                                            }
                                        } catch (_: Exception) { }
                                    }
                                }

                                // If there's an object representing the latest message, try to extract text + time
                                val objKeys = listOf("lastMessageObject", "latest", "latest_message", "last_message_object", "last_message_obj")
                                for (k in objKeys) {
                                    if (obj.has(k) && obj.get(k).isJsonObject) {
                                        val m = obj.getAsJsonObject(k)
                                        val textKeys = listOf("body", "message", "text", "content")
                                        var txt: String? = null
                                        for (tk in textKeys) if (m.has(tk) && !m.get(tk).isJsonNull) try { txt = m.get(tk).asString; break } catch (_: Exception) { }
                                        val timeKeys = listOf("createdAt","created_at","sentAt","sent_at","time","timestamp","date")
                                        var t: String? = null
                                        for (tk in timeKeys) if (m.has(tk) && !m.get(tk).isJsonNull) try { t = m.get(tk).asString; break } catch (_: Exception) { }
                                        if (!txt.isNullOrBlank()) return Pair(txt, t)
                                    }
                                }

                                // If there is an array of messages, try to pick the most recent by timestamp when possible,
                                // otherwise determine array ordering using first/last timestamps or fall back to last element.
                                val arrKeys = listOf("messages", "items", "results", "conversationMessages", "msgs")
                                for (k in arrKeys) {
                                    if (obj.has(k) && obj.get(k).isJsonArray) {
                                        val a = obj.getAsJsonArray(k)
                                        if (a.size() == 0) continue

                                        // Build a list of entries with text + parsed time (if present)
                                        data class Entry(val text: String?, val timeStr: String?, val timeMillis: Long?)
                                        val entries = mutableListOf<Entry>()
                                        for (j in 0 until a.size()) {
                                            val jelem = a[j]
                                            if (!jelem.isJsonObject) { entries.add(Entry(null, null, null)); continue }
                                            val mobj = jelem.asJsonObject
                                            var txt: String? = null
                                            for (tk in listOf("body","message","text","content","lastMessage")) if (mobj.has(tk) && !mobj.get(tk).isJsonNull) try { txt = mobj.get(tk).asString; break } catch (_: Exception) { }
                                            var timestr: String? = null
                                            for (tk in listOf("createdAt","created_at","sentAt","sent_at","time","timestamp","date")) if (mobj.has(tk) && !mobj.get(tk).isJsonNull) try { timestr = mobj.get(tk).asString; break } catch (_: Exception) { }
                                            var timeVal: Long? = null
                                            if (!timestr.isNullOrBlank()) {
                                                try { timeVal = timestr.toLong() } catch (_: Exception) {
                                                    try { timeVal = java.time.Instant.parse(timestr).toEpochMilli() } catch (_: Exception) { timeVal = null }
                                                }
                                            }
                                            entries.add(Entry(txt, timestr, timeVal))
                                        }

                                        // If any time values are parseable, pick the entry with max timestamp
                                        val anyTimes = entries.any { it.timeMillis != null }
                                        if (anyTimes) {
                                            val best = entries.maxByOrNull { it.timeMillis ?: Long.MIN_VALUE }
                                            if (best != null && !best.text.isNullOrBlank()) return Pair(best.text, best.timeStr)
                                        }

                                        // If no times parseable, try to detect ordering by comparing first/last timestamps when present
                                        if (entries.size >= 2) {
                                            val first = entries.first()
                                            val last = entries.last()
                                            val ftime = first.timeMillis
                                            val ltime = last.timeMillis
                                            if (ftime != null && ltime != null) {
                                                // if first >= last, array is newest-first; else newest is last
                                                return if (ftime >= ltime) Pair(first.text, first.timeStr) else Pair(last.text, last.timeStr)
                                            } else if (ftime != null) {
                                                return Pair(first.text, first.timeStr)
                                            } else if (ltime != null) {
                                                return Pair(last.text, last.timeStr)
                                            }
                                        }

                                        // No parseable timestamps found. Prefer the first element as a fallback because many
                                        // APIs return messages in newest-first order. If first is empty, fall back to last.
                                        val firstFallback = entries.firstOrNull()?.text
                                        if (!firstFallback.isNullOrBlank()) {
                                            // debug logging removed
                                            return Pair(firstFallback, null)
                                        }
                                        val lastFallback = entries.lastOrNull()?.text
                                        if (!lastFallback.isNullOrBlank()) {
                                            // debug logging removed
                                            return Pair(lastFallback, null)
                                        }
                                     }
                                }

                                // Nothing found
                            } catch (_: Exception) { }
                            return Pair(null, null)
                        }

                        // Helper: parse timestamp string to epoch millis (tries plain long, ISO instant)
                        fun parseTimeToMillis(t: String?): Long? {
                            if (t == null) return null
                            try {
                                // Try numeric epoch millis or seconds
                                val asLong = t.toLongOrNull()
                                if (asLong != null) {
                                    // Heuristic: if number looks like seconds (10 digits), convert to millis
                                    return if (asLong < 1000000000000L) asLong * 1000L else asLong
                                }
                            } catch (_: Exception) { }
                            try {
                                return java.time.Instant.parse(t).toEpochMilli()
                            } catch (_: Exception) { }
                            // Try common custom formats like "yyyy-MM-dd HH:mm:ss"
                            try {
                                val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                val ldt = java.time.LocalDateTime.parse(t, fmt)
                                return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            } catch (_: Exception) { }
                            // Try a variant with timezone offset if present
                            try {
                                val fmt2 = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                                val zdt = java.time.ZonedDateTime.parse(t, fmt2)
                                return zdt.toInstant().toEpochMilli()
                            } catch (_: Exception) { }
                            return null
                        }

                        if (jsonArr != null) {
                            for (i in 0 until jsonArr.size()) {
                                val elem = jsonArr[i]
                                try {
                                    val conv = gson.fromJson(elem, Conversation::class.java)
                                    if (conv != null) {
                                        var name = conv.partnerName

                                        // existing name inference logic preserved
                                        if (name.isNullOrBlank() && elem.isJsonObject) {
                                            val obj = elem.asJsonObject
                                            // Common candidate keys for partner name (many variants)
                                            val keys = listOf(
                                                "partnerName", "partner_name", "partner", "partnerFullName",
                                                "partner_full_name", "name", "displayName", "display_name",
                                                "fullName", "full_name", "firstName", "first_name", "first_name"
                                            )
                                            for (k in keys) {
                                                if (obj.has(k) && !obj.get(k).isJsonNull) {
                                                    try { name = obj.get(k).asString; break } catch (_: Exception) {}
                                                }
                                            }
                                            // Check nested 'partner' or 'user' objects
                                            if (name.isNullOrBlank()) {
                                                val nestedKeys = listOf("partner", "user", "customer", "owner", "sender", "receiver", "profile", "participant")
                                                for (nk in nestedKeys) {
                                                    if (obj.has(nk) && obj.get(nk).isJsonObject) {
                                                        val p = obj.getAsJsonObject(nk)
                                                        val pk = listOf("fullName", "full_name", "name", "displayName", "display_name", "firstName", "first_name", "first_name")
                                                        for (k in pk) {
                                                            if (p.has(k) && !p.get(k).isJsonNull) {
                                                                try { name = p.get(k).asString; break } catch (_: Exception) {}
                                                            }
                                                        }
                                                        // Try first+last name snake or camel
                                                        if (name.isNullOrBlank()) {
                                                            val firstKeys = listOf("firstName", "first_name", "firstname")
                                                            val lastKeys = listOf("lastName", "last_name", "lastname")
                                                            var f: String? = null
                                                            var l: String? = null
                                                            for (fk in firstKeys) if (p.has(fk) && !p.get(fk).isJsonNull) try { f = p.get(fk).asString } catch (_: Exception) {}
                                                            for (lk in lastKeys) if (p.has(lk) && !p.get(lk).isJsonNull) try { l = p.get(lk).asString } catch (_: Exception) {}
                                                            if (!f.isNullOrBlank() || !l.isNullOrBlank()) {
                                                                name = listOfNotNull(f?.trim(), l?.trim()).joinToString(" ").trim()
                                                            }
                                                        }
                                                        if (!name.isNullOrBlank()) break
                                                    }
                                                }
                                            }
                                        }

                                        // --- NEW: extract latest message preview and timestamp when available ---
                                        val (previewText, previewAt) = extractLatestMessage(elem)

                                        // Decide whether to override conv.lastMessage with the extracted preview.
                                        // Only override when:
                                        //  - conv.lastMessage is blank/null, OR
                                        //  - previewAt is parseable and newer than conv.lastMessageAt.
                                        var finalConv = conv
                                        try {
                                            val convTime = parseTimeToMillis(conv.lastMessageAt)
                                            val previewTime = parseTimeToMillis(previewAt)
                                            if (conv.lastMessage.isNullOrBlank()) {
                                                if (!previewText.isNullOrBlank()) finalConv = conv.copy(lastMessage = previewText, lastMessageAt = previewAt ?: conv.lastMessageAt)
                                            } else if (!previewText.isNullOrBlank() && previewTime != null) {
                                                // if preview timestamp is newer than conv timestamp, prefer preview
                                                if (convTime == null || previewTime > convTime) {
                                                    finalConv = conv.copy(lastMessage = previewText, lastMessageAt = previewAt ?: conv.lastMessageAt)
                                                }
                                            }
                                        } catch (_: Exception) {
                                            // fallback: do not override
                                            finalConv = conv
                                        }

                                        if (!name.isNullOrBlank() && name != finalConv.partnerName) {
                                            enriched.add(finalConv.copy(partnerName = name))
                                        } else {
                                            enriched.add(finalConv)
                                        }
                                    }
                                } catch (_: Exception) {
                                    // ignore parse error for this element
                                }
                            }
                        } else {
                            enriched.addAll(conversations)
                        }

                        if (enriched.isEmpty()) {
                            this@MessagesFragment.showEmpty()
                        } else {
                            tvEmpty?.visibility = View.GONE
                            // NOTE: we used to submit `enriched` here immediately, but that produced
                            // duplicate rows and sometimes showed older previews. The final
                            // collapsed/deduped/sorted list is submitted later in this function.
                            // So defer submitting until we've collapsed/sorted/deduped below.

                            // Fetching partner names is handled after we compute `deduped` and submit the final list below.
                        }

                        // Compute final deduped/sorted list for display
                        val deduped = enriched
                            .groupBy { it.partnerID }
                            .map { it.value.maxByOrNull { c -> parseTimeToMillis(c.lastMessageAt) ?: 0L } }
                            .filterNotNull()
                            .sortedWith(compareByDescending<Conversation> { c -> parseTimeToMillis(c.lastMessageAt) ?: 0L })

                        // Merge cached previews (saved by ChatFragment) so UI reflects recent local messages
                        val finalList: List<Conversation> = try {
                            val merged = deduped.map { conv ->
                                try {
                                    val preview = com.example.capstone2.repository.SharedPrefManager.getConversationPreview(requireContext(), conv.partnerID)
                                    if (preview != null) {
                                        val serverTime = parseTimeToMillis(conv.lastMessageAt)
                                        val previewTime = parseTimeToMillis(preview.lastMessageAt)
                                        if (!preview.lastMessage.isNullOrBlank() && (previewTime != null && (serverTime == null || previewTime > serverTime))) {
                                            // preview is newer; use it
                                            return@map conv.copy(lastMessage = preview.lastMessage, lastMessageAt = preview.lastMessageAt)
                                        }
                                    }
                                } catch (_: Exception) { }
                                conv
                            }
                            // Re-sort after merging
                            merged.sortedWith(compareByDescending<Conversation> { c -> parseTimeToMillis(c.lastMessageAt) ?: 0L })
                        } catch (_: Exception) {
                            deduped
                        }
                        adapter.submitList(finalList)

                        // Debug: prepare and log the final list submitted to the adapter
                        try {
                            val sb = StringBuilder()
                            sb.append("final-list: ")
                            for (c in finalList) {
                                sb.append("[id=${c.conversationID},partner=${c.partnerID},name=${c.partnerName},time=${c.lastMessageAt}] ")
                            }
                            // debug logging removed
                        } catch (_: Exception) { }

                        // Fetch missing partner names from API if any
                        val missingIds = deduped.filter { it.partnerName.isNullOrBlank() }.map { it.partnerID }.distinct()
                         if (missingIds.isNotEmpty()) {
                            // Simple in-memory cache for fetched user names
                            val userNameCache = mutableMapOf<Long, String>()

                            // Helper: try several endpoints to fetch a display name for a user id
                            suspend fun fetchDisplayNameForId(id: Long): String? {
                                try {
                                    // First try typed endpoint if available
                                    try {
                                        val uresp = api.getUser(id)
                                        if (uresp.isSuccessful) {
                                            val user = uresp.body()
                                            if (user != null) {
                                                val display = listOfNotNull(user.firstName.takeIf { it.isNotBlank() }, user.lastName.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { user.emailAddress }
                                                if (display.isNotBlank()) return display
                                            }
                                        }
                                    } catch (_: Exception) {
                                        // typed endpoint may not exist or fail; continue to raw attempts
                                    }

                                    // Candidate raw endpoints relative to base URL — try them in order
                                    val candidates = listOf(
                                        "api/user/$id",
                                        "api/users/$id",
                                        "api/customers/$id",
                                        "api/customer/$id",
                                        "api/owners/$id",
                                        "api/owner/$id",
                                        "api/profile/$id",
                                        "api/users/$id/profile",
                                        "api/customers/$id/profile"
                                    )

                                    for (path in candidates) {
                                        try {
                                            // debug logging removed
                                            val r = api.getRaw(path)
                                            if (!r.isSuccessful) {
                                                // debug logging removed
                                                continue
                                            }
                                            val raw = try { r.body()?.string() } catch (_: Exception) { null }
                                            if (raw.isNullOrBlank()) continue
                                            // debug logging removed

                                            try {
                                                val je2 = JsonParser.parseString(raw)
                                                fun extractName(elem: JsonElement?): String? {
                                                    if (elem == null) return null
                                                    if (elem.isJsonObject) {
                                                        val obj2 = elem.asJsonObject
                                                        val keys2 = listOf("name","displayName","display_name","fullName","full_name","firstName","first_name","firstname")
                                                        for (k2 in keys2) {
                                                            if (obj2.has(k2) && !obj2.get(k2).isJsonNull) {
                                                                try { val s = obj2.get(k2).asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
                                                            }
                                                        }
                                                        val firstKeys2 = listOf("firstName","first_name","firstname")
                                                        val lastKeys2 = listOf("lastName","last_name","lastname")
                                                        var f2: String? = null; var l2: String? = null
                                                        for (fk2 in firstKeys2) if (obj2.has(fk2) && !obj2.get(fk2).isJsonNull) try { f2 = obj2.get(fk2).asString } catch (_: Exception) {}
                                                        for (lk2 in lastKeys2) if (obj2.has(lk2) && !obj2.get(lk2).isJsonNull) try { l2 = obj2.get(lk2).asString } catch (_: Exception) {}
                                                        if (!f2.isNullOrBlank() || !l2.isNullOrBlank()) return listOfNotNull(f2?.trim(), l2?.trim()).joinToString(" ").trim()
                                                        val nested2 = listOf("user","partner","customer","owner","profile","data")
                                                        for (nk2 in nested2) {
                                                            if (obj2.has(nk2) && obj2.get(nk2).isJsonObject) {
                                                                val maybe = extractName(obj2.get(nk2))
                                                                if (!maybe.isNullOrBlank()) return maybe
                                                            }
                                                        }
                                                    }
                                                    if (elem.isJsonArray) {
                                                        val arr2 = elem.asJsonArray
                                                        if (arr2.size() > 0) {
                                                            val first = arr2[0]
                                                            return extractName(first)
                                                        }
                                                    }
                                                    return null
                                                }

                                                val found = extractName(je2)
                                                if (!found.isNullOrBlank()) return found
                                            } catch (_: Exception) {
                                                // parsing failed, try next
                                            }
                                        } catch (_: Exception) {
                                            // raw GET failed, try next
                                        }
                                    }
                                } catch (_: Exception) {
                                    // overall failure, return null
                                }
                                return null
                            }

                            for (id in missingIds) {
                                try {
                                    // debug logging removed
                                    val display = fetchDisplayNameForId(id)
                                    if (!display.isNullOrBlank()) {
                                        // debug logging removed
                                        userNameCache[id] = display
                                    } else {
                                        // debug logging removed
                                    }
                                } catch (_: Exception) {
                                    // ignore individual fetch errors
                                }
                            }

                            if (userNameCache.isNotEmpty()) {
                                val updated = deduped.map { c ->
                                    if (c.partnerName.isNullOrBlank() && userNameCache.containsKey(c.partnerID)) c.copy(partnerName = userNameCache[c.partnerID]) else c
                                }
                                val updatedSorted = updated.sortedWith(compareByDescending<Conversation> { c -> parseTimeToMillis(c.lastMessageAt) ?: 0L })
                                adapter.submitList(updatedSorted)
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "parse convs", e)
                        this@MessagesFragment.showEmpty()
                    }
                } else {
                    this@MessagesFragment.showEmpty()
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadConversations", e)
                this@MessagesFragment.showEmpty()
            }
        }
    }

    private fun showEmpty() {
        adapter.submitList(emptyList())
        tvEmpty?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        try { previewReceiver?.let { requireContext().unregisterReceiver(it) } } catch (_: Exception) {}
        previewReceiver = null
        super.onDestroyView()
    }
}
