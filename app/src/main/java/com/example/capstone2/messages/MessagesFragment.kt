package com.example.capstone2.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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

class MessagesFragment : Fragment() {

    private var rv: RecyclerView? = null
    private var tvEmpty: TextView? = null
    private lateinit var adapter: ConversationsAdapter

    companion object {
        private const val TAG = "MessagesFragment"
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
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "conversations raw: ${bodyStr?.take(2000)}")
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

                        if (jsonArr != null) {
                            for (i in 0 until jsonArr.size()) {
                                val elem = jsonArr[i]
                                try {
                                    val conv = gson.fromJson(elem, Conversation::class.java)
                                    if (conv != null) {
                                        var name = conv.partnerName
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

                                        if (!name.isNullOrBlank() && name != conv.partnerName) {
                                            enriched.add(conv.copy(partnerName = name))
                                        } else {
                                            enriched.add(conv)
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
                            // Submit initial list (with any inferred names)
                            adapter.submitList(enriched)

                            // Fetch missing partner names from API if any
                            val missingIds = enriched.filter { it.partnerName.isNullOrBlank() }.map { it.partnerID }.distinct()
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
                                                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "trying raw profile path: $path for id=$id")
                                                val r = api.getRaw(path)
                                                if (!r.isSuccessful) {
                                                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "raw path $path returned HTTP ${r.code()}")
                                                    continue
                                                }
                                                val raw = try { r.body()?.string() } catch (_: Exception) { null }
                                                if (raw.isNullOrBlank()) continue
                                                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "raw profile response for $path: ${raw.take(2000)}")

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
                                        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "fetching partner profile for userId=$id")
                                        val display = fetchDisplayNameForId(id)
                                        if (!display.isNullOrBlank()) {
                                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "fetched user $id display='$display'")
                                            userNameCache[id] = display
                                        } else {
                                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "no profile name found for id=$id")
                                        }
                                    } catch (_: Exception) {
                                        Log.w(TAG, "getUser($id) failed")
                                        // ignore individual fetch errors
                                    }
                                }

                                if (userNameCache.isNotEmpty()) {
                                    val updated = enriched.map { c ->
                                        if (c.partnerName.isNullOrBlank() && userNameCache.containsKey(c.partnerID)) c.copy(partnerName = userNameCache[c.partnerID]) else c
                                    }
                                    adapter.submitList(updated)
                                }
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
}
