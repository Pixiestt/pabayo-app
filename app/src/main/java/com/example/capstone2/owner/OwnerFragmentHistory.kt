package com.example.capstone2.owner

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
import com.example.capstone2.adapter.OwnerHistoryAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.viewmodel.OwnerHistoryViewModel
import com.example.capstone2.viewmodel.OwnerHistoryViewModelFactory
import com.example.capstone2.data.models.Request
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class OwnerFragmentHistory : Fragment(R.layout.owner_fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: OwnerHistoryAdapter
    private lateinit var historyViewModel: OwnerHistoryViewModel
    private lateinit var tvNoHistory: TextView

    // Date filter UI
    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnClear: Button

    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    // Local copy for filtering
    private var allCompletedRequests: List<Request> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerHistory)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)

        // New views
        etStartDate = view.findViewById(R.id.etStartDateHistoryOwner)
        etEndDate = view.findViewById(R.id.etEndDateHistoryOwner)
        btnFilter = view.findViewById(R.id.btnFilterHistoryOwner)
        btnClear = view.findViewById(R.id.btnClearFilterOwner)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = OwnerHistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        // Get token from shared preferences
        val sharedPreferences = requireContext().getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing auth token", Toast.LENGTH_SHORT).show()
            return
        }

        // Setup ViewModel
        val authedApiService = ApiClient.getApiService { token }
        val repository = RequestRepository(authedApiService)
        val viewModelFactory = OwnerHistoryViewModelFactory(repository)
        historyViewModel = ViewModelProvider(this, viewModelFactory)[OwnerHistoryViewModel::class.java]

        // Observe completed requests
        historyViewModel.completedRequests.observe(viewLifecycleOwner) { requests ->
            Log.d("OwnerHistory", "Received ${requests.size} completed requests in fragment")

            if (requests.isNotEmpty()) {
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

        // Fetch owner's completed requests
        fetchCompletedRequests()

        // Date pickers
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

        btnFilter.setOnClickListener { applyDateFilter() }
        btnClear.setOnClickListener {
            etStartDate.text.clear()
            etEndDate.text.clear()
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
        historyViewModel.fetchCompletedRequests()
    }

    override fun onResume() {
        super.onResume()
        fetchCompletedRequests()
    }

    private fun parseDateFromRequest(request: Request): Date? {
        val candidates = listOf(request.submittedAt, request.pickupDate, request.deliveryDate, request.dateUpdated, request.schedule)
        for (c in candidates) {
            if (!c.isNullOrBlank()) {
                val s = if (c.length >= 10) c.substring(0, 10) else c
                try {
                    return dateFormat.parse(s)
                } catch (e: Exception) {
                    // ignore
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
            Log.e("OwnerHistory", "Error parsing dates for filter", e)
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show()
        }
    }
}