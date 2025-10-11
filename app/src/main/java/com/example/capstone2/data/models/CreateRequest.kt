package com.example.capstone2.data.models

data class CreateRequest(
    val ownerID: Long,
    val customerID: Long,
    val serviceID: Long,
    val statusID: Long,
    val pickupDate: String?,
    val deliveryDate: String?,
    val sackQuantity: Int,
    val comment: String?,
    val modeID: Long,
    val pickupLocation: String?,
    val deliveryLocation: String?
)
