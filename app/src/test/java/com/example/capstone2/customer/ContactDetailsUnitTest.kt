package com.example.capstone2.customer

import com.example.capstone2.data.models.RequestWizardData
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactDetailsUnitTest {
    @Test
    fun testContactDetailsSubmission() {
        // Dummy info
        val dummyName = "John Doe"
        val dummyContact = "09123456789"
        val dummyComment = "Test comment"

        // Intended result
        val expectedName = "John Doe"
        val expectedContact = "09123456789"
        val expectedComment = "Test comment"

        // Simulate logic
        val wizardData = RequestWizardData()
        wizardData.customerName = dummyName
        wizardData.contactNumber = dummyContact
        wizardData.comment = dummyComment

        // Assert and print result
        assertEquals(expectedName, wizardData.customerName)
        assertEquals(expectedContact, wizardData.contactNumber)
        assertEquals(expectedComment, wizardData.comment)
        println("Unit test passed: Contact details submitted correctly!")
    }
}

