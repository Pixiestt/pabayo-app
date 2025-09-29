package com.example.capstone2.customer

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.CustomerHistoryAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.CustomerHistoryViewModel
import com.example.capstone2.viewmodel.CustomerHistoryViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.capstone2.data.models.Request

class CustomerFragmentHistory : Fragment(R.layout.customer_fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: CustomerHistoryAdapter
    private lateinit var historyViewModel: CustomerHistoryViewModel
    private lateinit var tvNoHistory: TextView
    private var customerID: Long = -1L

    // New UI for date filtering
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnClear: Button

    // Local copy of data for filtering
    private var allCompletedRequests: List<Request> = emptyList()

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)

        // New views
        etStartDate = view.findViewById(R.id.etStartDateHistoryCustomer)
        etEndDate = view.findViewById(R.id.etEndDateHistoryCustomer)
        btnFilter = view.findViewById(R.id.btnFilterHistoryCustomer)
        btnClear = view.findViewById(R.id.btnClearFilterCustomer)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = CustomerHistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        // Get token and customer ID from shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)
        customerID = sharedPreferences.getLong("userID", -1L)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        if (customerID == -1L) {
            Toast.makeText(requireContext(), "Customer ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = CustomerHistoryViewModelFactory(repository)
        historyViewModel = ViewModelProvider(this, viewModelFactory)[CustomerHistoryViewModel::class.java]

        // Observe completed requests
        historyViewModel.completedRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("CustomerHistory", "Received ${requests?.size ?: 0} completed requests in fragment")

            if (requests != null && requests.isNotEmpty()) {
                allCompletedRequests = requests
                historyAdapter.updateRequests(requests)
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            } else {
                allCompletedRequests = emptyList()
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "No completed requests found", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch customer's completed requests
        fetchCompletedRequests()

        // Date pickers for start/end
        val startPicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            etStartDate.setText(dateFormat.format(calendar.time))
        }
        val endPicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            etEndDate.setText(dateFormat.format(calendar.time))
        }

        etStartDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                startPicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        etEndDate.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                endPicker,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnFilter.setOnClickListener {
            applyDateFilter()
        }

        btnClear.setOnClickListener {
            etStartDate.text.clear()
            etEndDate.text.clear()
            // reset to full list
            historyAdapter.updateRequests(allCompletedRequests)
            if (allCompletedRequests.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            }
        }
    }

    private fun fetchCompletedRequests() {
        if (customerID != -1L) {
            historyViewModel.fetchCompletedRequests(customerID)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchCompletedRequests()
    }

    private fun parseDateFromRequest(request: Request): Date? {
        val candidates = listOf(request.submittedAt, request.pickupDate, request.deliveryDate, request.dateUpdated, request.schedule)
        for (c in candidates) {
            if (!c.isNullOrBlank()) {
                // try to extract yyyy-MM-dd portion
                val s = if (c.length >= 10) c.substring(0, 10) else c
                try {
                    return dateFormat.parse(s)
                } catch (e: Exception) {
                    // ignore and try next
                }
            }
        }
        return null
    }

    private fun applyDateFilter() {
        val startText = etStartDate.text.toString().trim()
        val endText = etEndDate.text.toString().trim()

        if (startText.isBlank() && endText.isBlank()) {
            historyAdapter.updateRequests(allCompletedRequests)
            return
        }

        try {
            val startDate: Date? = if (startText.isNotBlank()) dateFormat.parse(startText) else null
            val endDate: Date? = if (endText.isNotBlank()) dateFormat.parse(endText) else null

            val filtered = allCompletedRequests.filter { req ->
                val d = parseDateFromRequest(req) ?: return@filter false
                val afterStart = startDate?.let { !d.before(it) } ?: true
                val beforeEnd = endDate?.let { !d.after(it) } ?: true
                afterStart && beforeEnd
            }

            historyAdapter.updateRequests(filtered)

            recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
            tvNoHistory.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            Log.e("CustomerHistory", "Error parsing dates for filter", e)
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show()
        }
    }
}