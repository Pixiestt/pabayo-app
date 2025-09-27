// Inside onViewCreated
radioGroup.setOnCheckedChangeListener { _, checkedId ->
    when (checkedId) {
        R.id.rbPickupFromLocation -> {
            activity.getWizardData().pickupService = PickupService.PICKUP_FROM_LOCATION
            etPickupLocation.visibility = View.VISIBLE
        }
        R.id.rbDropOffAtFacility -> {
            activity.getWizardData().pickupService = PickupService.DROP_OFF_AT_FACILITY
            etPickupLocation.visibility = View.GONE
            etPickupLocation.text.clear() // Clear the text when hiding
        }
        else -> {
            etPickupLocation.visibility = View.GONE
            etPickupLocation.text.clear()
        }
    }
}

// Update the Next button click listener
btnNext.setOnClickListener {
    when {
        radioGroup.checkedRadioButtonId == -1 -> {
            // Show error message for no selection
            showToast("Please select a pickup option")
            return@setOnClickListener
        }
        activity.getWizardData().pickupService == PickupService.PICKUP_FROM_LOCATION && 
        etPickupLocation.text.toString().trim().isEmpty() -> {
            etPickupLocation.error = "Please enter your location"
            return@setOnClickListener
        }
        else -> {
            if (activity.getWizardData().pickupService == PickupService.PICKUP_FROM_LOCATION) {
                activity.getWizardData().pickupLocation = etPickupLocation.text.toString().trim()
            }
            activity.goToNextStep()
        }
    }
}