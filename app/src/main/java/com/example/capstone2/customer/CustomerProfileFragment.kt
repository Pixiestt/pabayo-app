package com.example.capstone2.customer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.capstone2.R
import com.example.capstone2.data.models.ChangePasswordRequest
import com.example.capstone2.data.models.UpdateProfileRequest
import com.example.capstone2.viewmodel.UserViewModel
import com.example.capstone2.viewmodel.UserViewModelFactory

class CustomerProfileFragment : Fragment(R.layout.fragment_customer_profile) {

    private lateinit var userViewModel: UserViewModel

    private lateinit var firstNameET: EditText
    private lateinit var lastNameET: EditText
    private lateinit var emailTV: TextView
    private lateinit var contactET: EditText
    private lateinit var addressET: EditText
    private lateinit var saveBtn: Button
    private lateinit var profileProgress: ProgressBar

    private lateinit var oldPassET: EditText
    private lateinit var newPassET: EditText
    private lateinit var newPassConfirmET: EditText
    private lateinit var changePassBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_customer_profile, container, false)

        firstNameET = view.findViewById(R.id.firstNameET)
        lastNameET = view.findViewById(R.id.lastNameET)
        emailTV = view.findViewById(R.id.emailTV)
        contactET = view.findViewById(R.id.contactET)
        addressET = view.findViewById(R.id.addressET)
        saveBtn = view.findViewById(R.id.saveBtn)
        profileProgress = view.findViewById(R.id.profileProgress)

        oldPassET = view.findViewById(R.id.oldPassET)
        newPassET = view.findViewById(R.id.newPassET)
        newPassConfirmET = view.findViewById(R.id.newPassConfirmET)
        changePassBtn = view.findViewById(R.id.changePassBtn)

        userViewModel = ViewModelProvider(
            requireActivity(),
            UserViewModelFactory(requireContext())
        )[UserViewModel::class.java]

        observeProfile()

        saveBtn.setOnClickListener {
            saveProfile()
        }

        changePassBtn.setOnClickListener {
            changePassword()
        }

        return view
    }

    private fun observeProfile() {
        userViewModel.getProfile().observe(viewLifecycleOwner) { user ->
            if (user != null) {
                firstNameET.setText(user.firstName)
                lastNameET.setText(user.lastName)
                emailTV.text = user.emailAddress
                contactET.setText(user.contactNumber)
                addressET.setText(user.homeAddress)
            } else {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateProfile(): Boolean {
        var ok = true
        // First name required
        val first = firstNameET.text.toString().trim()
        if (first.isEmpty()) {
            firstNameET.error = getString(R.string.error_first_name_required)
            ok = false
        } else {
            firstNameET.error = null
        }

        // Last name required
        val last = lastNameET.text.toString().trim()
        if (last.isEmpty()) {
            lastNameET.error = getString(R.string.error_last_name_required)
            ok = false
        } else {
            lastNameET.error = null
        }

        // Contact required and must be exactly 11 digits
        val contact = contactET.text.toString().trim()
        if (contact.isEmpty()) {
            contactET.error = getString(R.string.error_contact_required)
            ok = false
        } else if (!contact.matches(Regex("^\\d{11}$"))) {
            contactET.error = getString(R.string.error_contact_invalid)
            ok = false
        } else {
            contactET.error = null
        }

        // Address required
        val address = addressET.text.toString().trim()
        if (address.isEmpty()) {
            addressET.error = getString(R.string.error_address_required)
            ok = false
        } else {
            addressET.error = null
        }

        return ok
    }

    private fun setUiBusy(busy: Boolean) {
        if (busy) {
            profileProgress.visibility = View.VISIBLE
            saveBtn.isEnabled = false
            changePassBtn.isEnabled = false
        } else {
            profileProgress.visibility = View.GONE
            saveBtn.isEnabled = true
            changePassBtn.isEnabled = true
        }
    }

    private fun saveProfile() {
        if (!validateProfile()) return

        val req = UpdateProfileRequest(
            firstNameET.text.toString().trim().ifEmpty { null },
            lastNameET.text.toString().trim().ifEmpty { null },
            null, // Email is display-only; do not allow changing it here
            contactET.text.toString().trim().ifEmpty { null },
            addressET.text.toString().trim().ifEmpty { null }
        )

        setUiBusy(true)
        userViewModel.updateProfile(req).observe(viewLifecycleOwner) { updatedUser ->
            setUiBusy(false)
            if (updatedUser != null) {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun changePassword() {
        val oldPass = oldPassET.text.toString()
        val newPass = newPassET.text.toString()
        val confirmPass = newPassConfirmET.text.toString()

        // Basic validation: both required and new password length >= 8
        var ok = true
        if (oldPass.isBlank()) {
            oldPassET.error = getString(R.string.error_old_password_required)
            ok = false
        } else {
            oldPassET.error = null
        }
        if (newPass.length < 8) {
            newPassET.error = getString(R.string.error_new_password_short)
            ok = false
        } else {
            newPassET.error = null
        }
        // Confirm password matches
        if (confirmPass != newPass) {
            newPassConfirmET.error = getString(R.string.error_password_mismatch)
            ok = false
        } else {
            newPassConfirmET.error = null
        }

        if (!ok) return

        // Show confirmation dialog
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.action_change_password))
            .setMessage(getString(R.string.saving_password))
            .setPositiveButton("Yes") { _, _ ->
                setUiBusy(true)
                val req = ChangePasswordRequest(oldPass, newPass)
                userViewModel.changePassword(req).observe(viewLifecycleOwner) { resp ->
                    setUiBusy(false)
                    if (resp != null) {
                        Toast.makeText(requireContext(), "Password changed", Toast.LENGTH_SHORT).show()
                        oldPassET.text.clear()
                        newPassET.text.clear()
                        newPassConfirmET.text.clear()
                    } else {
                        Toast.makeText(requireContext(), "Password change failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}
