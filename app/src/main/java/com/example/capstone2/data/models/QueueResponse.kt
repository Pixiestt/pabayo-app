package com.example.capstone2.data.models

data class QueueResponse(
    val pending: List<Request> = emptyList(),
    val processing: List<Request> = emptyList()
)

