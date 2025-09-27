package com.example.capstone2.data.models

data class RequestWizardData(
    // Step 1: Details of Rice Milling
    var millingType: MillingType? = null,
    
    // Step 2: Pick up Service

    var pickupService: PickupService? = null,
    var pickupLocation: String? = null,
    
    // Step 3: Delivery Service
    var deliveryService: DeliveryService? = null,
    var deliveryLocation: String? = null,
    
    // Step 4: Feeds Conversion
    var feedsConversion: Boolean? = null,
    
    // Step 5: Contact Details
    var customerName: String? = null,
    var contactNumber: String? = null,
    
    // Additional data
    var sackCount: Int = 1,
    var pickupDate: String? = null,
    var comment: String? = null
)

enum class MillingType {
    MILLING_FOR_FEE,
    MILLING_FOR_RICE_CONVERSION
}

enum class PickupService {
    PICKUP_FROM_LOCATION,
    DROP_OFF_AT_FACILITY
}

enum class DeliveryService {
    DELIVER_TO_LOCATION,
    PICKUP_FROM_FACILITY
}
