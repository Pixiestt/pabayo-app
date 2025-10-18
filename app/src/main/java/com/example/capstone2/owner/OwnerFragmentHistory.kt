package com.example.capstone2.owner

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.capstone2.R
import com.example.capstone2.adapter.OwnerHistoryAdapter
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.RequestRepository
import com.example.capstone2.repository.SharedPrefManager
import com.example.capstone2.viewmodel.OwnerHistoryViewModel
import com.example.capstone2.viewmodel.OwnerHistoryViewModelFactory
import com.example.capstone2.data.models.Request
import android.Manifest
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Material date range picker + java.time for robust date handling
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.Instant
import java.time.ZoneOffset

class OwnerFragmentHistory : Fragment(R.layout.owner_fragment_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: OwnerHistoryAdapter
    private lateinit var historyViewModel: OwnerHistoryViewModel
    private lateinit var tvNoHistory: TextView

    // Filter action buttons
    private lateinit var btnFilter: Button
    private lateinit var btnClear: Button
    private lateinit var btnExportPdf: Button

    // Filter state (selected range + queries)
    private var selectedStartDate: Date? = null
    private var selectedEndDate: Date? = null
    private var selectedNameQuery: String? = null
    private var selectedIdQuery: String? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    // Local copy for filtering
    private var allCompletedRequests: List<Request> = emptyList()

    // Permission handling for export
    private val REQ_WRITE_EXTERNAL_STORAGE = 201
    private var pendingExportAfterPermission = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewOwnerHistory)
        tvNoHistory = view.findViewById(R.id.tvNoHistory)

        // Buttons
        btnFilter = view.findViewById(R.id.btnFilterHistoryOwner)
        btnClear = view.findViewById(R.id.btnClearFilterOwner)
        btnExportPdf = view.findViewById(R.id.btnExportPdfOwner)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        historyAdapter = OwnerHistoryAdapter(emptyList())
        recyclerView.adapter = historyAdapter

        // Get token from shared preferences
        val token = SharedPrefManager.getAuthToken(requireContext())

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
                applyFilters() // respect any existing filters
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            } else {
                allCompletedRequests = emptyList()
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
                Toast.makeText(requireContext(), getString(R.string.no_completed_requests), Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch owner's completed requests
        fetchCompletedRequests()

        // Filter button opens a filter options menu
        btnFilter.setOnClickListener {
            showFilterMenu()
        }

        btnClear.setOnClickListener {
            selectedStartDate = null
            selectedEndDate = null
            selectedNameQuery = null
            selectedIdQuery = null
            historyAdapter.updateRequests(allCompletedRequests)
            if (allCompletedRequests.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvNoHistory.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                tvNoHistory.visibility = View.GONE
            }
        }

        // Export button
        btnExportPdf.setOnClickListener {
            maybeExportPdf()
        }
    }

    private fun showFilterMenu() {
        val options = arrayOf(
            getString(R.string.filter_by_date_range),
            getString(R.string.filter_by_customer_name),
            getString(R.string.filter_by_request_id)
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.action_filter))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openDateRangePicker()
                    1 -> promptForCustomerName()
                    2 -> promptForRequestId()
                }
            }
            .show()
    }

    private fun openDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.filter_by_date_range))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            if (selection != null) {
                val startMillis = selection.first
                val endMillis = selection.second
                if (startMillis != null && endMillis != null) {
                    // Normalize to start of day UTC to align with yyyy-MM-dd parsing
                    selectedStartDate = Date.from(
                        Instant.ofEpochMilli(startMillis).atZone(ZoneOffset.UTC)
                            .toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)
                    )
                    selectedEndDate = Date.from(
                        Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC)
                            .toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC)
                    )
                    applyFilters()
                }
            }
        }
        picker.show(parentFragmentManager, "owner_history_date_range")
    }

    private fun promptForCustomerName() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.enter_customer_name)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setText(selectedNameQuery ?: "")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.filter_by_customer_name))
            .setView(input)
            .setPositiveButton(R.string.apply) { _, _ ->
                selectedNameQuery = input.text.toString().trim().ifBlank { null }
                applyFilters()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.action_clear) { _, _ ->
                selectedNameQuery = null
                applyFilters()
            }
            .show()
    }

    private fun promptForRequestId() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.enter_request_id)
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(selectedIdQuery ?: "")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.filter_by_request_id))
            .setView(input)
            .setPositiveButton(R.string.apply) { _, _ ->
                selectedIdQuery = input.text.toString().trim().ifBlank { null }
                applyFilters()
            }
            .setNegativeButton(R.string.cancel, null)
            .setNeutralButton(R.string.action_clear) { _, _ ->
                selectedIdQuery = null
                applyFilters()
            }
            .show()
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
                } catch (_: Exception) {
                    // ignore
                }
            }
        }
        return null
    }

    private fun applyFilters() {
        val startDate = selectedStartDate
        val endDate = selectedEndDate
        val nameQuery = selectedNameQuery
        val idQuery = selectedIdQuery

        if (startDate == null && endDate == null && nameQuery == null && idQuery == null) {
            historyAdapter.updateRequests(allCompletedRequests)
            return
        }

        try {
            val filtered = allCompletedRequests.filter { req ->
                val d = parseDateFromRequest(req)
                val afterStart = startDate?.let { date -> d?.let { !it.before(date) } ?: false } ?: true
                val beforeEnd = endDate?.let { date -> d?.let { !it.after(date) } ?: false } ?: true
                val nameOk = nameQuery?.let { q -> req.customerName.contains(q, ignoreCase = true) } ?: true
                val idOk = idQuery?.let { q -> req.requestID.toString().contains(q, ignoreCase = true) } ?: true
                afterStart && beforeEnd && nameOk && idOk
            }

            historyAdapter.updateRequests(filtered)

            recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
            tvNoHistory.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        } catch (e: Exception) {
            Log.e("OwnerHistory", "Error applying filters", e)
            Toast.makeText(requireContext(), "Invalid filters", Toast.LENGTH_SHORT).show()
        }
    }

    private fun maybeExportPdf() {
        val currentList = historyAdapter.getRequests()
        if (currentList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.export_pdf_empty), Toast.LENGTH_SHORT).show()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val hasPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                pendingExportAfterPermission = true
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQ_WRITE_EXTERNAL_STORAGE)
                Toast.makeText(requireContext(), getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show()
                return
            }
        }
        exportPdf(currentList)
    }

    private fun exportPdf(requests: List<Request>) {
        try {
            val sdfFile = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val fileName = "owner_history_${sdfFile.format(Date())}.pdf"

            // Build PDF document
            val doc = PdfDocument()
            val pageWidth = 595 // A4 width in points (approx)
            val pageHeight = 842 // A4 height in points (approx)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
            }
            val linePaint = Paint().apply {
                strokeWidth = 1f
            }

            var pageNumber = 1
            var y = 0f
            lateinit var currentPage: PdfDocument.Page
            lateinit var canvas: Canvas

            fun startNewPage() {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = doc.startPage(pageInfo)
                canvas = currentPage.canvas
                y = 40f
                // Title
                canvas.drawText("Capstone – Owner Transaction History", 40f, y, titlePaint)
                y += 30f
                // Header row
                var x = 40f
                canvas.drawText("ID", x, y, headerPaint); x += 50f
                canvas.drawText("Customer", x, y, headerPaint); x += 160f
                canvas.drawText("Service", x, y, headerPaint); x += 130f
                canvas.drawText("Date", x, y, headerPaint); x += 110f
                canvas.drawText("Sacks", x, y, headerPaint); x += 60f
                canvas.drawText("Amount", x, y, headerPaint)
                // underline
                y += 6f
                canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, linePaint)
                y += 12f
            }

            startNewPage()

            for (req in requests) {
                if (y > pageHeight - 60) {
                    doc.finishPage(currentPage)
                    pageNumber++
                    startNewPage()
                }
                var x = 40f
                canvas.drawText(req.requestID.toString(), x, y, textPaint); x += 50f
                canvas.drawText(if (req.customerName.length <= 18) req.customerName else req.customerName.substring(0,17) + "…", x, y, textPaint); x += 160f
                canvas.drawText(if (req.serviceName.length <= 16) req.serviceName else req.serviceName.substring(0,15) + "…", x, y, textPaint); x += 130f
                val dateStr = req.dateUpdated?.let { if (it.length >= 10) it.substring(0,10) else it }
                    ?: req.deliveryDate?.let { if (it.length >= 10) it.substring(0,10) else it }
                    ?: req.submittedAt?.let { if (it.length >= 10) it.substring(0,10) else it }
                    ?: ""
                canvas.drawText(dateStr, x, y, textPaint); x += 110f
                canvas.drawText(req.sackQuantity.toString(), x, y, textPaint); x += 60f
                val amt: Double? = req.paymentAmount
                    ?: req.payment?.amount
                    ?: req.payment?.amountString?.toDoubleOrNull()
                val amtStr = if (amt != null) "₱" + String.format(Locale.getDefault(), "%.2f", amt) else "-"
                canvas.drawText(amtStr, x, y, textPaint)
                y += 18f
            }

            // Close last page
            doc.finishPage(currentPage)

            // Save to storage
            val outputStream: OutputStream?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Capstone")
                }
                val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val savedUri = resolver.insert(collection, contentValues)
                outputStream = savedUri?.let { resolver.openOutputStream(it) }
            } else {
                val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dir = File(downloads, "Capstone").apply { if (!exists()) mkdirs() }
                val outFile = File(dir, fileName)
                outputStream = FileOutputStream(outFile)
            }

            outputStream.use { os ->
                if (os == null) throw IllegalStateException("No output stream available")
                doc.writeTo(os)
            }
            doc.close()

            val shownName = fileName
            Toast.makeText(requireContext(), getString(R.string.export_pdf_success, shownName), Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("OwnerHistory", "Failed to export PDF", e)
            Toast.makeText(requireContext(), getString(R.string.export_pdf_error), Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_WRITE_EXTERNAL_STORAGE) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted && pendingExportAfterPermission) {
                pendingExportAfterPermission = false
                val list = historyAdapter.getRequests()
                if (list.isNotEmpty()) exportPdf(list)
            } else {
                pendingExportAfterPermission = false
            }
        }
    }
}
