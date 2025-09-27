package com.example.capstone2.customer

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.capstone2.data.models.RequestWizardData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDetailsFragmentTest {
    class TestRequestWizardActivity : RequestWizardActivity() {
        var submitCalled = false
        override fun submitRequest() {
            submitCalled = true
            super.submitRequest()
        }
    }

    @get:Rule
    val activityRule = ActivityScenarioRule(TestRequestWizardActivity::class.java)

    @Test
    fun testSubmitButtonSendsDetailsToActivity() {
        activityRule.scenario.onActivity { activity ->
            activity.showStep(5) // Show ContactDetailsFragment
        }
        // Simulate user input
        onView(withId(R.id.etCustomerName)).perform(replaceText("John Doe"))
        onView(withId(R.id.etContactNumber)).perform(replaceText("09123456789"))
        onView(withId(R.id.etComment)).perform(replaceText("Test comment"))
        // Click submit
        onView(withId(R.id.btnSubmit)).perform(click())
        // Check that submitRequest was called
        activityRule.scenario.onActivity { activity ->
            assert((activity as TestRequestWizardActivity).submitCalled)
            val wizardData = activity.getWizardData()
            assertEquals("John Doe", wizardData.customerName)
            assertEquals("09123456789", wizardData.contactNumber)
            assertEquals("Test comment", wizardData.comment)
        }
    }
}
