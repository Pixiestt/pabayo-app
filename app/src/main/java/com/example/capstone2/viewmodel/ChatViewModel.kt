package com.example.capstone2.viewmodel

import androidx.lifecycle.*
import com.example.capstone2.data.api.ApiService
import com.example.capstone2.data.models.Message
import com.example.capstone2.data.models.SendMessageRequest
import kotlinx.coroutines.launch

class ChatViewModel(private val apiService: ApiService, private val currentUserId: Long) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>(emptyList())
    val messages: LiveData<List<Message>> = _messages

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadConversation(conversationID: String? = null, otherUserID: Long? = null) {
        viewModelScope.launch {
            try {
                val resp = apiService.getConversation(conversationID = conversationID, otherUserID = otherUserID)
                if (resp.isSuccessful) {
                    _messages.postValue(resp.body() ?: emptyList())
                } else {
                    _error.postValue("Failed to load conversation: ${resp.code()}")
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
                val resp = apiService.sendMessage(req)
                if (resp.isSuccessful) {
                    val created = resp.body()
                    if (created != null) {
                        val current = _messages.value?.toMutableList() ?: mutableListOf()
                        current.add(created)
                        _messages.postValue(current)
                    }
                } else {
                    _error.postValue("Failed to send message: ${resp.code()}")
                }
            } catch (e: Exception) {
                _error.postValue(e.message)
            }
        }
    }
}

