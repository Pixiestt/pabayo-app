package com.example.capstone2.viewmodel

import androidx.lifecycle.*
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.Message
import com.example.capstone2.data.models.SendMessageRequest
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import android.util.Log

class ChatViewModel(private val apiService: ApiService, private val currentUserId: Long) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // New: debug LiveData to show last request/response in UI when adb isn't available
    private val _debug = MutableLiveData<String?>(null)
    val debug: LiveData<String?> = _debug

    fun loadConversation(conversationID: String? = null, otherUserID: Long? = null) {
        viewModelScope.launch {
            try {
                val resp = apiService.getConversation(conversationID = conversationID, otherUserID = otherUserID)

                // Read both body and errorBody (reading them may consume them) and post debug immediately
                val responseBodyStr = try { resp.body()?.string() } catch (e: Exception) { null }
                val responseErrStr = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                val respBodyShort = responseBodyStr ?: "<empty>"
                val respErrShort = responseErrStr ?: "<none>"
                _debug.postValue("Conversation HTTP ${resp.code()}: body=$respBodyShort err=$respErrShort")

                if (!resp.isSuccessful) {
                    _error.postValue("Failed to load conversation: ${resp.code()}")
                    return@launch
                }

                val bodyStr = responseBodyStr
                val bodyShort = bodyStr ?: "<empty>"
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "getConversation raw response: ${bodyStr?.take(4000)}")
                }
                _debug.postValue("Conversation response (userId=$currentUserId):\n$bodyShort")

                if (bodyStr.isNullOrBlank()) {
                    _messages.postValue(emptyList())
                    return@launch
                }

                try {
                    val gson = Gson()
                    val je = JsonParser.parseString(bodyStr)

                    // Helper: recursively find first array element in a JSON tree
                    fun findFirstArray(elem: JsonElement): JsonElement? {
                        if (elem == null) return null
                        if (elem.isJsonArray) return elem
                        if (elem.isJsonObject) {
                            val obj = elem.asJsonObject
                            val keys = listOf("messages", "data", "results", "items", "conversation", "body")
                            for (k in keys) {
                                if (obj.has(k)) {
                                    val child = obj.get(k)
                                    if (child.isJsonArray) return child
                                    val nested = findFirstArray(child)
                                    if (nested != null) return nested
                                }
                            }
                            val it = obj.entrySet().iterator()
                            while (it.hasNext()) {
                                val entry = it.next()
                                val found = findFirstArray(entry.value)
                                if (found != null) return found
                            }
                        }
                        return null
                    }

                    // Helper extractors (block-bodied so `return` is permitted inside)
                    fun JsonElement.asSafeString(): String? {
                        return try {
                            if (this.isJsonNull) null else this.asString
                        } catch (e: Exception) {
                            null
                        }
                    }

                    fun JsonElement.asSafeLong(): Long? {
                        return try {
                            if (this.isJsonNull) return null
                            val p = this.asJsonPrimitive
                            if (p.isNumber) return p.asLong
                            p.asString.toLongOrNull()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    fun getStringFromObj(obj: com.google.gson.JsonObject, candidates: List<String>): String? {
                        for (k in candidates) {
                            if (obj.has(k) && !obj.get(k).isJsonNull) {
                                val v = try { obj.get(k) } catch (e: Exception) { null }
                                if (v != null) {
                                    val s = v.asSafeString()
                                    if (!s.isNullOrBlank()) return s
                                }
                            }
                        }
                        return null
                    }

                    fun getLongFromObj(obj: com.google.gson.JsonObject, candidates: List<String>): Long? {
                        for (k in candidates) {
                            if (obj.has(k) && !obj.get(k).isJsonNull) {
                                val v = try { obj.get(k) } catch (e: Exception) { null }
                                if (v != null) {
                                    val l = v.asSafeLong()
                                    if (l != null) return l
                                }
                            }
                        }
                        return null
                    }

                    // Parse array case
                    val firstArray = findFirstArray(je)
                    if (firstArray != null && firstArray.isJsonArray) {
                        val arr = firstArray.asJsonArray
                        val built = mutableListOf<Message>()

                        for (i in 0 until arr.size()) {
                            val elem = arr[i]
                            try {
                                if (elem.isJsonObject) {
                                    val obj = elem.asJsonObject
                                    val messageText = getStringFromObj(obj, listOf("message","body","text","content","messageText","msg","message_body"))
                                    val idVal = getLongFromObj(obj, listOf("messageID","id","msg_id"))
                                    val senderVal = getLongFromObj(obj, listOf("senderID","senderId","from","userID","userId"))
                                    val receiverVal = getLongFromObj(obj, listOf("receiverID","receiverId","to"))
                                    val created = getStringFromObj(obj, listOf("createdAt","timestamp","time","created_at"))
                                    val conv = getStringFromObj(obj, listOf("conversationID","conversationId","conversation"))
                                    val senderName = getStringFromObj(obj, listOf("senderName","sender_name","name","displayName","display_name"))
                                    val receiverName = getStringFromObj(obj, listOf("receiverName","receiver_name","receiver","toName","to","receiverDisplayName","receiver_display_name"))

                                    val msg = Message(
                                        id = idVal,
                                        senderID = senderVal ?: -1L,
                                        receiverID = receiverVal ?: -1L,
                                        message = messageText ?: "",
                                        timestamp = created,
                                        conversationID = conv,
                                        senderName = senderName,
                                        receiverName = receiverName
                                    )
                                    built.add(msg)
                                } else {
                                    // not an object: attempt to parse with Gson fallback
                                    try {
                                        val parsed = gson.fromJson(elem, Message::class.java)
                                        if (parsed != null) built.add(parsed)
                                    } catch (_: Exception) {
                                        // ignore
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore element-level errors
                            }
                        }

                        _messages.postValue(built)
                        return@launch
                    }

                    // Single object heuristics
                    if (je.isJsonObject) {
                        val obj = je.asJsonObject
                        if (obj.has("senderID") || obj.has("message") || obj.has("id") || obj.has("conversationID")) {
                            val single = try { gson.fromJson(obj, Message::class.java) } catch (e: Exception) { null }
                            if (single != null) {
                                _messages.postValue(listOf(single))
                                return@launch
                            }
                        }
                    }

                    // Final fallback: try parse the body as List<Message>
                    try {
                        val messagesArray = try { gson.fromJson(bodyStr, Array<Message>::class.java) } catch (e: Exception) { null }
                        if (messagesArray != null) {
                            _messages.postValue(messagesArray.toList())
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse body as messages array")
                        val emsg = e.message ?: "<null>"
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Exception while parsing list: " + emsg)
                            Log.d(TAG, "Raw response (full): " + (bodyStr ?: "<empty>"))
                        }
                        _messages.postValue(emptyList())
                        return@launch
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse messages JSON")
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Exception while parsing messages JSON: " + (e.message ?: "<null>"))
                        Log.d(TAG, "Raw response (full): " + (bodyStr ?: "<empty>"))
                    }
                    _error.postValue("Failed to parse messages: " + (e.message ?: ""))
                    return@launch
                }

            } catch (e: Exception) {
                _error.postValue(e.message)
            }
        }
    }

    fun sendMessage(receiverID: Long, text: String, conversationID: String? = null) {
        viewModelScope.launch {
            try {
                val req = SendMessageRequest(receiverID = receiverID, message = text, conversationID = conversationID)
                // Debug: outgoing JSON
                try {
                    val gson = Gson()
                    val outJson = gson.toJson(req)
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "sendMessage request JSON: $outJson")
                    }
                    _debug.postValue("Outgoing request:\n$outJson")
                } catch (e: Exception) {}

                // Primary send
                // The project's ApiService exposes `sendMessageRaw` which accepts a Map payload.
                // Build a raw payload map from the request and use that for the primary send.
                val rawPayload = mutableMapOf<String, Any>("message" to text, "receiverID" to receiverID)
                conversationID?.let { rawPayload["conversationID"] = it }
                val resp = apiService.sendMessageRaw(rawPayload)
                if (resp.isSuccessful) {
                    val bodyStr = try { resp.body()?.string() } catch (e: Exception) { null }
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "sendMessage response raw: ${bodyStr?.take(2000)}")
                    }
                    if (!bodyStr.isNullOrBlank()) {
                         try {
                             val gson = Gson()
                             val je = JsonParser.parseString(bodyStr)
                             var parsedMessage: Message? = null
                             if (je.isJsonObject) {
                                 val obj = je.asJsonObject
                                 if (obj.has("data")) {
                                     try { parsedMessage = gson.fromJson(obj.get("data"), Message::class.java) } catch (e: Exception) {}
                                 }
                                 if (parsedMessage == null) {
                                     try { parsedMessage = gson.fromJson(obj, Message::class.java) } catch (e: Exception) {}
                                 }
                             } else if (je.isJsonArray) {
                                 try {
                                     val arr = je.asJsonArray
                                     if (arr.size() > 0) parsedMessage = gson.fromJson(arr[0], Message::class.java)
                                 } catch (e: Exception) {}
                             }

                             if (parsedMessage != null) {
                                 val current = _messages.value?.toMutableList() ?: mutableListOf()
                                 current.add(parsedMessage)
                                 _messages.postValue(current)
                                 _debug.postValue("Sent OK. Created message id=${parsedMessage.id}")
                                 // refresh authoritative state
                                 loadConversation(conversationID = conversationID, otherUserID = receiverID)
                             } else {
                                 // fallback: refresh conversation to pull the message
                                 _debug.postValue((_debug.value ?: "") + "\nWarning: couldn't parse created message from server response")
                                 loadConversation(conversationID = conversationID, otherUserID = receiverID)
                             }
                         } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse sendMessage response: " + (e.message ?: "<null>"))
                             _debug.postValue((_debug.value ?: "") + "\nFailed to parse send response: " + (e.message ?: ""))
                             loadConversation(conversationID = conversationID, otherUserID = receiverID)
                         }
                     } else {
                         // empty body, refresh
                         loadConversation(conversationID = conversationID, otherUserID = receiverID)
                     }
                 } else {
                     // Non-successful primary response
                     val errBody = try { resp.errorBody()?.string() } catch (e: Exception) { null }
                     Log.e(TAG, "sendMessage failed with HTTP ${resp.code()} body=${errBody}")
                     val errShort = errBody ?: "<no body>"
                     _debug.postValue("Server error: HTTP ${resp.code()}\n$errShort")

                     if (resp.code() == 422) {
                         // validation: try a raw retry with camelCase keys
                         val rawMap = mutableMapOf<String, Any>("message" to text, "receiverID" to receiverID)
                         conversationID?.let { rawMap["conversationID"] = it }
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                             Log.d(TAG, "Attempting raw send retry with body: $rawMap")
                         }
                         _debug.postValue((_debug.value ?: "") + "\nRetrying with raw body: $rawMap")

                         try {
                             val retryResp = apiService.sendMessageRaw(rawMap)
                             if (retryResp.isSuccessful) {
                                val bodyStr = try { retryResp.body()?.string() } catch (e: Exception) { null }
                                if (Log.isLoggable(TAG, Log.DEBUG)) {
                                    Log.d(TAG, "retry raw response: ${bodyStr?.take(2000)}")
                                }
                                 _debug.postValue((_debug.value ?: "") + "\nRetry response: " + (bodyStr ?: "<empty>"))

                                 if (!bodyStr.isNullOrBlank()) {
                                     try {
                                         val gson = Gson()
                                         val je = JsonParser.parseString(bodyStr)
                                         var maybe: Message? = null
                                         if (je.isJsonObject) {
                                             val obj = je.asJsonObject
                                             if (obj.has("data")) {
                                                 try { maybe = gson.fromJson(obj.get("data"), Message::class.java) } catch (e: Exception) {}
                                             }
                                             if (maybe == null) {
                                                 try { maybe = gson.fromJson(obj, Message::class.java) } catch (e: Exception) {}
                                             }
                                         } else if (je.isJsonArray) {
                                             try { val arr = je.asJsonArray; if (arr.size() > 0) maybe = gson.fromJson(arr[0], Message::class.java) } catch (e: Exception) {}
                                         }

                                         if (maybe != null) {
                                             val current = _messages.value?.toMutableList() ?: mutableListOf()
                                             current.add(maybe)
                                             _messages.postValue(current)
                                             _debug.postValue((_debug.value ?: "") + "\nRetry parsed message id=${maybe.id}")
                                             loadConversation(conversationID = conversationID, otherUserID = receiverID)
                                             return@launch
                                         } else {
                                             // couldn't parse but HTTP OK: refresh
                                             loadConversation(conversationID = conversationID, otherUserID = receiverID)
                                         }
                                     } catch (e: Exception) {
                                         Log.e(TAG, "retry parse exception", e)
                                         // fallthrough to error handling below
                                     }
                                 } else {
                                     // empty retry body: refresh
                                     loadConversation(conversationID = conversationID, otherUserID = receiverID)
                                 }
                             } else {
                                val retryErr = try { retryResp.errorBody()?.string() } catch (e: Exception) { null }
                                Log.e(TAG, "raw retry failed HTTP ${retryResp.code()} body=${retryErr}")
                                 val userVisible = buildString {
                                     append("Failed to send message: ${resp.code()}")
                                     if (!errBody.isNullOrBlank()) {
                                         append(" - ")
                                         append(if (errBody.length > 1000) errBody.take(1000) + "..." else errBody)
                                     }
                                     if (!retryErr.isNullOrBlank()) {
                                         append(" | Retry: ")
                                         append(if (retryErr.length > 1000) retryErr.take(1000) + "..." else retryErr)
                                     }
                                 }
                                 _error.postValue(userVisible)
                                 val retryErrShort = retryErr ?: "<no body>"
                                 _debug.postValue((_debug.value ?: "") + "\nRetry failed: $retryErrShort")
                                 return@launch
                             }
                         } catch (e: Exception) {
                            Log.e(TAG, "raw retry exception: " + (e.message ?: "<null>"))
                             _error.postValue(errBody ?: e.message ?: "Failed to send message")
                             _debug.postValue((_debug.value ?: "") + "\nRetry exception: " + (e.message ?: ""))
                             return@launch
                         }
                     } else {
                         // For 500/internal errors, show server status and let maintainer fix it
                         _error.postValue("Server returned HTTP ${resp.code()}")
                         return@launch
                     }
                 }
             } catch (e: Exception) {
                Log.e(TAG, "sendMessage exception: " + (e.message ?: "<null>"))
                 _error.postValue(e.message)
                 _debug.postValue((_debug.value ?: "") + "\nException: " + (e.message ?: ""))
             }
         }
     }
 }
