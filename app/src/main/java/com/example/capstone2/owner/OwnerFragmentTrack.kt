package com.example.capstone2.owner

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.RequestAdapter
import com.example.capstone2.adapter.TrackAdapter
import com.example.capstone2.data.models.Request
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.OwnerRequestViewModel
import com.example.capstone2.viewmodel.OwnerRequestViewModelFactory
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OwnerFragmentTrack : Fragment(R.layout.owner_fragment_track) {

    private lateinit var trackAdapter: TrackAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var requestAdapter: RequestAdapter
    private lateinit var ownerRequestViewModel: OwnerRequestViewModel

    // Simple in-memory cache: userId -> contactNumber
    private val contactCache = mutableMapOf<Long, String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerTrack)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        requestAdapter = RequestAdapter(emptyList()) { /* No action needed for accepted list */ }
        recyclerView.adapter = requestAdapter

        // Get token
        val token = SharedPrefManager.getAuthToken(requireContext())

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = OwnerRequestViewModelFactory(repository, requireActivity().application)
        ownerRequestViewModel = ViewModelProvider(this, viewModelFactory)[OwnerRequestViewModel::class.java]

        // Provide a lifecycle-aware fetchContact callback with a simple in-memory cache
        val fetchContactCallback: (Long, (String) -> Unit) -> Unit = { customerId, onResult ->
            // If cached, return immediately on main thread
            val cached = contactCache[customerId]
            if (cached != null) {
                Log.d("OwnerFragmentTrack", "fetchContact: cache hit for $customerId -> $cached")
                onResult(cached)
            } else {
                // Otherwise fetch using lifecycleScope to avoid leaks
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val api = ApiClient.getApiService { token }

                        Log.d("OwnerFragmentTrack", "fetchContact: trying typed getUser for $customerId")

                        // First try typed endpoint
                        try {
                            val resp = api.getUser(customerId)
                            if (resp.isSuccessful) {
                                val contact = resp.body()?.contactNumber?.takeIf { s -> s.isNotBlank() }
                                if (!contact.isNullOrBlank()) {
                                    contactCache[customerId] = contact
                                    withContext(Dispatchers.Main) { onResult(contact) }
                                    return@launch
                                }
                            }
                        } catch (_: Exception) {
                            // typed endpoint may not exist or return unexpected shape; continue to raw attempts
                        }

                        // Raw fallback: try multiple candidate endpoints and attempt lenient parsing
                        val candidates = listOf(
                            "api/user/$customerId",
                            "api/users/$customerId",
                            "api/customers/$customerId",
                            "api/customer/$customerId",
                            "api/owners/$customerId",
                            "api/owner/$customerId",
                            "api/profile/$customerId",
                            "api/users/$customerId/profile",
                            "api/customers/$customerId/profile",
                            "api/profiles/$customerId"
                        )

                        var found: String? = null
                        var rawSnippet: String? = null
                        var rawPath: String? = null

                        for (path in candidates) {
                            try {
                                Log.d("OwnerFragmentTrack", "fetchContact: trying raw path $path")
                                val r = api.getRaw(path)
                                if (!r.isSuccessful) {
                                    Log.d("OwnerFragmentTrack", "fetchContact: raw path $path unsuccessful, code=${r.code()}")
                                    continue
                                }
                                val raw = try { r.body()?.string() } catch (_: Exception) { null }
                                if (raw.isNullOrBlank()) {
                                    Log.d("OwnerFragmentTrack", "fetchContact: raw path $path returned empty body")
                                    continue
                                }

                                // Save a short sanitized snippet for debugging if we fail to extract
                                if (rawSnippet == null) {
                                    rawSnippet = raw.replace(Regex("[\\r\\n]"), " ").trim().take(200)
                                    rawPath = path
                                }

                                val root = try { JsonParser.parseString(raw) } catch (_: Exception) { null }
                                val extracted = extractContactRecursive(root)
                                if (!extracted.isNullOrBlank()) {
                                    found = extracted
                                    Log.d("OwnerFragmentTrack", "fetchContact: extracted contact from $path")
                                    break
                                }
                            } catch (ex: Exception) {
                                Log.d("OwnerFragmentTrack", "fetchContact: raw path $path threw: ${ex.message}")
                                // ignore and continue
                            }
                        }

                        val final = found?.takeIf { it.isNotBlank() } ?: run {
                            // If we have a raw snippet, include a terse hint for debugging
                            if (!rawSnippet.isNullOrBlank()) {
                                val p = rawPath ?: "unknown"
                                "Not available (server returned at $p: ${rawSnippet})"
                            } else {
                                "Not available"
                            }
                        }
                        contactCache[customerId] = final
                        Log.d("OwnerFragmentTrack", "fetchContact: final for $customerId -> $final")
                        withContext(Dispatchers.Main) { onResult(final) }
                    } catch (ex: Exception) {
                        Log.d("OwnerFragmentTrack", "fetchContact: overall failure for $customerId: ${ex.message}")
                        withContext(Dispatchers.Main) { onResult("Not available") }
                    }
                }
            }
        }

        trackAdapter = TrackAdapter(emptyList(), { request, newStatusID ->
            // Call a function to update status
            updateRequestStatus(request, newStatusID)
        }, fetchContactCallback)

        recyclerView.adapter = trackAdapter

        ownerRequestViewModel.ownerRequests.observe(viewLifecycleOwner) { requests ->
            val filteredRequests = requests?.filterNot { it.statusID in listOf(1, 8, 9) }
            // Reorder so that the earliest accepted request (statusID == 10) is placed at the top
            val reordered = filteredRequests?.let { reorderRequests(it) }
            trackAdapter.updateRequests(reordered.orEmpty())


            if (reordered.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No requests found matching the filter", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch requests
        ownerRequestViewModel.fetchOwnerRequests()
    }

    private fun updateRequestStatus(request: Request, newStatusID: Int) {
        // If updating to Milling done, prompt for payment amount first
        if (newStatusID == 12) {
            showAmountDialogAndProceed(request)
            return
        }
        ownerRequestViewModel.updateStatus(request.requestID, newStatusID,
            onSuccess = {
                Toast.makeText(requireContext(), "Status updated", Toast.LENGTH_SHORT).show()
                ownerRequestViewModel.fetchOwnerRequests() // 🔁 Refresh from backend
            },
            onError = { error ->
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun showAmountDialogAndProceed(request: Request) {
        val ctx = requireContext()
        val input = android.widget.EditText(ctx).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = getString(R.string.enter_amount_hint)
        }
        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, 0)
            addView(input)
        }
        androidx.appcompat.app.AlertDialog.Builder(ctx)
            .setTitle(getString(R.string.set_payment_amount_title))
            .setMessage(getString(R.string.set_payment_amount_message))
            .setView(container)
            .setPositiveButton(R.string.confirm) { dialog, _ ->
                val text = input.text?.toString()?.trim().orEmpty()
                val amount = text.toDoubleOrNull()
                if (amount == null || amount < 0.0) {
                    Toast.makeText(ctx, getString(R.string.invalid_amount_message), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                // Proceed: set amount then update status to 12
                ownerRequestViewModel.setPaymentAmountThenUpdateStatus(
                    requestID = request.requestID,
                    amount = amount,
                    newStatusID = 12,
                    onSuccess = {
                        Toast.makeText(ctx, getString(R.string.payment_amount_set_and_status_updated), Toast.LENGTH_SHORT).show()
                        ownerRequestViewModel.fetchOwnerRequests()
                    },
                    onError = { err ->
                        Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                    }
                )
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // Recursively search JsonElement for common contact/phone fields and return the first match
    private fun extractContactRecursive(elem: JsonElement?): String? {
        if (elem == null || elem.isJsonNull) return null
        try {
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                // keys likely to hold contact numbers
                val keys = listOf("contactNumber", "contact_number", "contact", "contactNo", "contactno", "contact_no", "phone", "phoneNumber", "phone_number", "phone_no", "mobile", "mobileNumber", "mobile_number", "mobile_no", "tel", "telephone", "telephoneNumber", "telephone_no", "msisdn", "phoneNumberFormatted")
                for (k in keys) {
                    if (obj.has(k) && !obj.get(k).isJsonNull) {
                        try { val s = obj.get(k).asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
                    }
                }

                // Also try name components in case contact is nested under 'user' or 'profile'
                val nested = listOf("user", "partner", "customer", "owner", "profile", "data", "attributes")
                for (nk in nested) {
                    if (obj.has(nk) && obj.get(nk).isJsonObject) {
                        val maybe = extractContactRecursive(obj.get(nk))
                        if (!maybe.isNullOrBlank()) return maybe
                    }
                }

                // If no nested object, check for arrays inside object
                for ((_, v) in obj.entrySet()) {
                    if (v.isJsonArray) {
                        val arr = v.asJsonArray
                        for (el in arr) {
                            val maybe = extractContactRecursive(el)
                            if (!maybe.isNullOrBlank()) return maybe
                        }
                    }
                }
            }

            if (elem.isJsonArray) {
                val arr = elem.asJsonArray
                for (el in arr) {
                    val maybe = extractContactRecursive(el)
                    if (!maybe.isNullOrBlank()) return maybe
                }
            }

            // As a last resort, if element is primitive, try to get string
            if (elem.isJsonPrimitive) {
                try { val s = elem.asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Reorder list so the earliest accepted request (statusID == 10) appears first.
     * "Earliest" is determined using dateUpdated, falling back to submittedAt, then lexicographic order if needed.
     */
    private fun reorderRequests(list: List<Request>): List<Request> {
        if (list.isEmpty()) return list
        val accepted = list.filter { it.statusID == 10 }
        if (accepted.isEmpty()) return list
        // Choose earliest by (dateUpdated ?: submittedAt ?: "") using lexicographic compare (assuming ISO-like strings)
        val firstAccepted = accepted.minByOrNull { (it.dateUpdated ?: it.submittedAt) ?: "" }
            ?: return list
        if (list.firstOrNull()?.requestID == firstAccepted.requestID) return list // already at top
        val remainder = list.filter { it.requestID != firstAccepted.requestID }
        return listOf(firstAccepted) + remainder
    }
}
