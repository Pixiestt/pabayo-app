package com.example.capstone2.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class PaymentInfo(
    // primary amount key
    @SerializedName(value = "amount", alternate = ["paymentAmount", "payment_amount", "price", "total", "total_amount"]) val amount: Double? = null,
    // sometimes servers send strings for numeric fields
    @SerializedName(value = "amount_str", alternate = ["amountString", "amount_text"]) val amountString: String? = null
) : Parcelable

@Parcelize
data class Request(
    val requestID: Long,
    val ownerID: Long,
    val customerID: Long,
    val customerName: String,
    val serviceID: Long,
    val serviceName: String,
    var statusID: Int,
    val pickupDate: String?,
    val paymentMethod: String,
    val deliveryDate: String?,
    val sackQuantity: Int,
    val comment: String?,
    val dateUpdated: String?,
    val schedule: String?,
    val submittedAt: String?,
    val pickupLocation: String?,
    val deliveryLocation: String?,
    // optional fields returned by API when available
    val feedsConversion: Boolean? = null,
    val millingType: String? = null,
    // optional contact number for customer (may not be provided by server)
    val contactNumber: String? = null,
    // NEW: optional pickup time in HH:mm
    val pickupTime: String? = null,
    // NEW: optional payment amount set by owner when Milling done; support multiple backend keys
    @SerializedName(value = "paymentAmount", alternate = ["payment_amount", "amount", "price", "total_amount"]) val paymentAmount: Double? = null,
    // NEW: alternatively, some backends nest the payment details
    val payment: PaymentInfo? = null,
    // NEW: store milled rice weight (kg) if provided by backend
    @SerializedName(value = "milledKg", alternate = ["milled_kg", "milledWeightKg", "milled_weight_kg"]) val milledKg: Double? = null
) : Parcelable
