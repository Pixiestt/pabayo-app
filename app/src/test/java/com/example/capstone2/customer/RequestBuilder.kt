package com.example.capstone2.customer

import com.example.capstone2.data.models.RequestWizardData
import com.example.capstone2.data.models.CreateRequest

fun buildCreateRequest(
    data: RequestWizardData,
    customerID: Long,
    ownerID: Long = 2L,
    serviceID: Long = 1L,
    statusID: Long = 1L,
    modeID: Long = 1L
): CreateRequest {
    return CreateRequest(
        ownerID = ownerID,
        customerID = customerID,
        serviceID = serviceID,
        statusID = statusID,
        pickupDate = data.pickupDate ?: "",
        pickupTime = data.pickupTime,
        deliveryDate = null,
        sackQuantity = data.sackCount,
        comment = data.comment ?: "",
        modeID = modeID,
        pickupLocation = data.pickupLocation,
        deliveryLocation = data.deliveryLocation
    )
}
