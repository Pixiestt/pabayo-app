package com.example.capstone2.user

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.capstone2.R
import com.example.capstone2.network.ApiClient
import com.example.capstone2.repository.SharedPrefManager
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewUserProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvRawHint: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnRetry: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_user_profile)

        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvContact = findViewById(R.id.tvContact)
        tvAddress = findViewById(R.id.tvAddress)
        tvRawHint = findViewById(R.id.tvRawHint)
        progress = findViewById(R.id.profileProgress)
        btnRetry = findViewById(R.id.btnRetry)

        val userId = intent.getLongExtra("userId", -1L)
        val rawHint = intent.getStringExtra("rawHint")
        if (!rawHint.isNullOrBlank()) tvRawHint.text = rawHint

        if (userId <= 0L) {
            Toast.makeText(this, "Invalid user id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        btnRetry.setOnClickListener { loadProfile(userId) }
        loadProfile(userId)
    }

    private fun loadProfile(userId: Long) {
        progress.visibility = View.VISIBLE
        tvName.text = ""
        tvEmail.text = ""
        tvContact.text = ""
        tvAddress.text = ""

        val token = SharedPrefManager.getAuthToken(this)
        val api = ApiClient.getApiService { token }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val resp = api.getUser(userId)
                if (resp.isSuccessful) {
                    val user = resp.body()
                    // If typed parsing succeeded but fields are absent/null, attempt a raw fetch
                    // so we can show the server-returned payload for debugging.
                    var rawFallbackHint: String? = null
                    var extractedContactFromRaw: String? = null
                    var extractedFirstName: String? = null
                    var extractedLastName: String? = null
                    var extractedEmail: String? = null
                    var extractedAddress: String? = null
                     if (user != null) {
                         val allBlank = listOf(user.firstName, user.lastName, user.emailAddress, user.contactNumber, user.homeAddress).all { it.isNullOrBlank() }
                         if (allBlank) {
                            // Try a list of candidate endpoints (server may expose user data under several paths)
                            val candidates = listOf(
                                "api/user/$userId",
                                "api/users/$userId",
                                "api/customers/$userId",
                                "api/customer/$userId",
                                "api/owners/$userId",
                                "api/owner/$userId",
                                "api/profile/$userId",
                                "api/users/$userId/profile",
                                "api/customers/$userId/profile",
                                "api/profiles/$userId"
                            )

                            for (p in candidates) {
                                val rawRespFallback = try { api.getRaw(p) } catch (_: Exception) { null }
                                val rawBodyFallback = try { rawRespFallback?.body()?.string() } catch (_: Exception) { null }
                                if (rawBodyFallback.isNullOrBlank()) continue
                                // keep first non-empty raw for debugging
                                if (rawFallbackHint == null) {
                                    rawFallbackHint = try { JsonParser.parseString(rawBodyFallback).toString() } catch (_: Exception) { rawBodyFallback }
                                }

                                val root = try { JsonParser.parseString(rawBodyFallback) } catch (_: Exception) { null }
                                if (root == null) continue

                                // Try to extract common user fields from the raw JSON
                                extractedFirstName = extractedFirstName ?: extractStringRecursive(root, listOf("firstName", "firstname", "first_name", "givenName", "given_name"))
                                extractedLastName = extractedLastName ?: extractStringRecursive(root, listOf("lastName", "lastname", "last_name", "familyName", "family_name"))
                                extractedEmail = extractedEmail ?: extractStringRecursive(root, listOf("emailAddress", "email", "email_address"))
                                extractedContactFromRaw = extractedContactFromRaw ?: extractContactRecursive(root)
                                extractedAddress = extractedAddress ?: extractStringRecursive(root, listOf("homeAddress", "home_address", "address", "streetAddress"))

                                // stop early if we found something usable for contact or email/name
                                if (!extractedContactFromRaw.isNullOrBlank() || !extractedEmail.isNullOrBlank() || !extractedFirstName.isNullOrBlank()) break
                            }
                         }
                     }
                     withContext(Dispatchers.Main) {
                         progress.visibility = View.GONE
                         if (user != null) {
                             // Use safe calls because firstName/lastName are nullable
                             val fullName = listOfNotNull(user.firstName?.takeIf { it.isNotBlank() }, user.lastName?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { "-" }
                            // Prefer typed values, fallback to extracted raw values when typed user fields are blank
                            val displayFirst = user.firstName ?: extractedFirstName
                            val displayLast = user.lastName ?: extractedLastName
                            val displayFullName = listOfNotNull(displayFirst?.takeIf { it.isNotBlank() }, displayLast?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { fullName }
                            tvName.text = displayFullName
                            tvEmail.text = user.emailAddress ?: extractedEmail ?: "-"
                             // Prefer typed user contact if present; otherwise use extracted contact from raw JSON
                            tvContact.text = user.contactNumber ?: extractedContactFromRaw ?: getString(R.string.not_available)
                            tvAddress.text = user.homeAddress ?: extractedAddress ?: "-"
                             // If we fetched a raw fallback (server returned nulls), show a short snippet
                             if (!rawFallbackHint.isNullOrBlank()) {
                                 tvRawHint.text = getString(R.string.server_returned_raw, rawFallbackHint.take(1000))
                             }
                         } else {
                             tvContact.text = getString(R.string.not_available)
                             Toast.makeText(this@ViewUserProfileActivity, "Profile payload empty", Toast.LENGTH_SHORT).show()
                         }
                     }
                 } else {
                    // Try raw fallback to show what server returns for debugging
                    val rawResp = try { api.getRaw("api/users/$userId") } catch (_: Exception) { null }
                    val rawBody = try { rawResp?.body()?.string() } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) {
                        progress.visibility = View.GONE
                        tvContact.text = getString(R.string.not_available)
                        if (!rawBody.isNullOrBlank()) {
                            // rawBody is non-null and non-blank here, parse directly and fallback to rawBody on error
                            val pretty = try { JsonParser.parseString(rawBody).toString() } catch (_: Exception) { rawBody }
                            // pretty is non-null here (rawBody was checked); safely take up to 1000 chars
                            tvRawHint.text = getString(R.string.server_returned_raw, pretty.take(1000))
                        } else {
                            Toast.makeText(this@ViewUserProfileActivity, "Failed to load profile (code=${rawResp?.code()})", Toast.LENGTH_SHORT).show()
                        }
                    }
                 }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    progress.visibility = View.GONE
                    tvContact.text = getString(R.string.not_available)
                    Toast.makeText(this@ViewUserProfileActivity, "Network error: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Recursively search JsonElement for common contact/phone fields and return the first match
    private fun extractContactRecursive(elem: com.google.gson.JsonElement?): String? {
        if (elem == null || elem.isJsonNull) return null
        try {
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                val keys = listOf("contactNumber", "contact_number", "contact", "contactNo", "contactno", "contact_no", "phone", "phoneNumber", "phone_number", "phone_no", "mobile", "mobileNumber", "mobile_number", "mobile_no", "tel", "telephone", "telephoneNumber", "telephone_no", "msisdn", "phoneNumberFormatted")
                for (k in keys) {
                    if (obj.has(k) && !obj.get(k).isJsonNull) {
                        try { val s = obj.get(k).asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
                    }
                }

                val nested = listOf("user", "partner", "customer", "owner", "profile", "data", "attributes")
                for (nk in nested) {
                    if (obj.has(nk) && obj.get(nk).isJsonObject) {
                        val maybe = extractContactRecursive(obj.get(nk))
                        if (!maybe.isNullOrBlank()) return maybe
                    }
                }

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

            if (elem.isJsonPrimitive) {
                try { val s = elem.asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return null
    }

    // Recursively search JsonElement for the first matching keys and return the string
    private fun extractStringRecursive(elem: com.google.gson.JsonElement?, keys: List<String>): String? {
        if (elem == null || elem.isJsonNull) return null
        try {
            if (elem.isJsonObject) {
                val obj = elem.asJsonObject
                for (k in keys) {
                    if (obj.has(k) && !obj.get(k).isJsonNull) {
                        try { val s = obj.get(k).asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
                    }
                }
                val nested = listOf("user", "profile", "data", "attributes", "customer", "owner")
                for (nk in nested) {
                    if (obj.has(nk) && obj.get(nk).isJsonObject) {
                        val maybe = extractStringRecursive(obj.get(nk), keys)
                        if (!maybe.isNullOrBlank()) return maybe
                    }
                }
                for ((_, v) in obj.entrySet()) {
                    if (v.isJsonArray) {
                        for (el in v.asJsonArray) {
                            val maybe = extractStringRecursive(el, keys)
                            if (!maybe.isNullOrBlank()) return maybe
                        }
                    }
                }
            }
            if (elem.isJsonArray) {
                for (el in elem.asJsonArray) {
                    val maybe = extractStringRecursive(el, keys)
                    if (!maybe.isNullOrBlank()) return maybe
                }
            }
            if (elem.isJsonPrimitive) {
                try { val s = elem.asString; if (!s.isNullOrBlank()) return s } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return null
    }
}
