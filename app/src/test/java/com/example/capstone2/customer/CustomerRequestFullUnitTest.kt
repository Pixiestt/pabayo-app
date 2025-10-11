package com.example.capstone2.customer

import com.example.capstone2.data.models.RequestWizardData
import com.example.capstone2.data.models.MillingType
import com.example.capstone2.data.models.PickupService
import com.example.capstone2.data.models.DeliveryService
import com.example.capstone2.data.models.CreateRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerRequestFullUnitTest {
    @Test
    fun testFullCustomerRequestSubmission() {
        // Step 1: Milling Details
        val dummyMillingType = MillingType.MILLING_FOR_FEE
        val dummySackCount = 5

        // Step 2: Pickup Service
        val dummyPickupService = PickupService.PICKUP_FROM_LOCATION
        val dummyPickupLocation = "Kalaw"
        val dummyPickupDate = "2025-09-23"

        // Step 3: Delivery Service
        val dummyDeliveryService = DeliveryService.DELIVER_TO_LOCATION
        val dummyDeliveryLocation = "Adamson"

        // Step 4: Feeds Conversion
        val dummyFeedsConversion = true

        // Step 5: Contact Details
        val dummyName = "Michael Jackson"
        val dummyContact = "09987654321"
        val dummyComment = "Xiao long bao"

        // Simulate logic
        val wizardData = RequestWizardData()
        wizardData.millingType = dummyMillingType
        wizardData.sackCount = dummySackCount
        wizardData.pickupService = dummyPickupService
        wizardData.pickupLocation = dummyPickupLocation
        wizardData.pickupDate = dummyPickupDate
        wizardData.deliveryService = dummyDeliveryService
        wizardData.deliveryLocation = dummyDeliveryLocation
        wizardData.feedsConversion = dummyFeedsConversion
        wizardData.customerName = dummyName
        wizardData.contactNumber = dummyContact
        wizardData.comment = dummyComment

        // Build the request using your actual logic
        val customerID = 123L
        val request = buildCreateRequest(wizardData, customerID)

        // Assert all fields
        assertEquals(2L, request.ownerID)
        assertEquals(customerID, request.customerID)
        assertEquals(1L, request.serviceID)
        assertEquals(1L, request.statusID)
        assertEquals(dummyPickupDate, request.pickupDate)
        assertEquals(null, request.deliveryDate)
        assertEquals(dummySackCount, request.sackQuantity)
        assertEquals(dummyComment, request.comment)
        assertEquals(1L, request.modeID)
        assertEquals(dummyPickupLocation, request.pickupLocation)
        assertEquals(dummyDeliveryLocation, request.deliveryLocation)
        println("Unit test passed: Full customer request submitted and built correctly!")
    }
}
