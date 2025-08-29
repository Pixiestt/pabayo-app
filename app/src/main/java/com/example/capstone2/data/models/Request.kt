package com.example.capstone2.data.models

data class Request(
    val requestID: Long,
    val ownerID: Long,
    val customerID: Long,
    val customerName: String,
    val serviceID: Long,
    val serviceName: String,
    val courierID: Long,
    val statusID: Int,
    val pickupDate: String?,
    val paymentMethod: String,
    val deliveryDate: String?,
    val sackQuantity: Int,
    val comment: String?,
    val dateUpdated: String?,
    val schedule: String?,
    val submittedAt: String?,
    val pickupLocation: String?,
    val deliveryLocation: String?
)
