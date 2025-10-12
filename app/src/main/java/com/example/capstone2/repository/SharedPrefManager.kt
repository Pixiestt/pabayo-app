package com.example.capstone2.repository

import android.content.Context

object SharedPrefManager {
    private const val PREFS_NAME = "capstone_prefs"
    private val LEGACY_NAMES = arrayOf(PREFS_NAME, "MyAppPrefs")

    /**
     * Safely returns the stored userID from known preference files if present.
     * Accepts values stored as Long, Int, String (numeric), Float, Double.
     */
    fun getUserId(ctx: Context): Long? {
        val names = LEGACY_NAMES
        for (n in names) {
            val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
            if (!p.contains("userID")) continue
            val v = p.all["userID"]
            when (v) {
                is Long -> return v
                is Int -> return v.toLong()
                is String -> return v.toLongOrNull()
                is Float -> return v.toLong()
                is Double -> return v.toLong()
                else -> continue
            }
        }
        return null
    }

    /**
     * Persist the auth token into both the canonical and legacy preference files so
     * all parts of the app (old and new) can read it.
     */
    fun saveAuthToken(ctx: Context, token: String) {
        try {
            // write to canonical prefs
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putString("auth_token", token).apply()
        } catch (_: Exception) { }

        try {
            // write to legacy prefs for compatibility
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().putString("auth_token", token).apply()
        } catch (_: Exception) { }
    }

    /**
     * Try to find the auth token in any of the known preference files.
     */
    fun getAuthToken(ctx: Context): String? {
        val names = LEGACY_NAMES
        for (n in names) {
            val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
            val t = p.getString("auth_token", null)
            if (!t.isNullOrBlank()) return t
        }
        return null
    }

    /**
     * Persist the user id into both known preference files for compatibility.
     */
    fun saveUserId(ctx: Context, userId: Long) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putLong("userID", userId).apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().putLong("userID", userId).apply()
        } catch (_: Exception) { }
    }

    /**
     * Persist the user's account status under both known prefs files.
     * If status is null, saves a sensible default of "approved".
     */
    fun saveUserStatus(ctx: Context, status: String?) {
        val valueToSave = status ?: "approved"
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putString("user_status", valueToSave).apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().putString("user_status", valueToSave).apply()
        } catch (_: Exception) { }
    }

    /**
     * Try to return a user-friendly name for the currently-signed-in user from shared prefs.
     * Checks several common keys (`userFullName`, `displayName`, `firstName`/`lastName`) and
     * returns the first non-empty value found.
     */
    fun getUserFullName(ctx: Context): String? {
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val candidates = listOf(
            p.getString("userFullName", null),
            p.getString("displayName", null),
            p.getString("userName", null)
        ).filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }
        if (candidates.isNotEmpty()) return candidates.first()

        // Try to assemble from first/last name if available
        val first = p.getString("firstName", null)
        val last = p.getString("lastName", null)
        if (!first.isNullOrBlank() || !last.isNullOrBlank()) {
            return listOfNotNull(first?.trim(), last?.trim()).joinToString(" ")
        }

        return null
    }

    /**
     * Try to return the user status from shared preferences.
     * Checks both the canonical and legacy preference files and returns the first non-empty value found.
     * If no value is found, returns "approved" as a default.
     */
    fun getUserStatus(ctx: Context): String {
        val names = LEGACY_NAMES
        for (n in names) {
            try {
                val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
                val s = p.getString("user_status", null)
                if (!s.isNullOrBlank()) return s
            } catch (_: Exception) { }
        }
        return "approved"
    }

    // --- debug panel preference helpers ---
    private const val KEY_DEBUG_PANEL_VISIBLE = "debug_panel_visible"

    fun isDebugPanelEnabled(ctx: Context): Boolean {
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return p.getBoolean(KEY_DEBUG_PANEL_VISIBLE, false)
    }

    fun setDebugPanelEnabled(ctx: Context, enabled: Boolean) {
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        p.edit().putBoolean(KEY_DEBUG_PANEL_VISIBLE, enabled).apply()
    }

    // --- Conversation preview cache helpers (local optimistic UI) ---
    data class ConversationPreview(val conversationID: String?, val lastMessage: String?, val lastMessageAt: String?)

    private fun keyConv(partnerId: Long) = "preview_conv_${'$'}{partnerId}"
    private fun keyMsg(partnerId: Long) = "preview_msg_${'$'}{partnerId}"
    private fun keyAt(partnerId: Long) = "preview_at_${'$'}{partnerId}"

    fun saveConversationPreview(ctx: Context, partnerId: Long, conversationID: String?, lastMessage: String?, lastMessageAt: String?) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val e = p.edit()
            if (conversationID == null) e.remove(keyConv(partnerId)) else e.putString(keyConv(partnerId), conversationID)
            if (lastMessage == null) e.remove(keyMsg(partnerId)) else e.putString(keyMsg(partnerId), lastMessage)
            if (lastMessageAt == null) e.remove(keyAt(partnerId)) else e.putString(keyAt(partnerId), lastMessageAt)
            e.apply()
        } catch (_: Exception) { }
    }

    fun getConversationPreview(ctx: Context, partnerId: Long): ConversationPreview? {
        return try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val conv = p.getString(keyConv(partnerId), null)
            val msg = p.getString(keyMsg(partnerId), null)
            val at = p.getString(keyAt(partnerId), null)
            if (conv == null && msg == null && at == null) null else ConversationPreview(conv, msg, at)
        } catch (_: Exception) { null }
    }

    fun clearConversationPreview(ctx: Context, partnerId: Long) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().remove(keyConv(partnerId)).remove(keyMsg(partnerId)).remove(keyAt(partnerId)).apply()
        } catch (_: Exception) { }
    }

    fun clearAuthToken(ctx: Context) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().remove("auth_token").apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().remove("auth_token").apply()
        } catch (_: Exception) { }
    }

    fun clearUserId(ctx: Context) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().remove("userID").apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().remove("userID").apply()
        } catch (_: Exception) { }
    }

    // --- Unread messages count helpers ---
    private const val KEY_UNREAD_COUNT = "unread_messages_count"

    fun getUnreadMessagesCount(ctx: Context): Int {
        // Prefer canonical prefs, fall back to legacy
        val names = LEGACY_NAMES
        for (n in names) {
            try {
                val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
                if (p.contains(KEY_UNREAD_COUNT)) return p.getInt(KEY_UNREAD_COUNT, 0)
            } catch (_: Exception) { }
        }
        return 0
    }

    fun saveUnreadMessagesCount(ctx: Context, count: Int) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putInt(KEY_UNREAD_COUNT, count).apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().putInt(KEY_UNREAD_COUNT, count).apply()
        } catch (_: Exception) { }
    }

    // --- Local notifications flag helpers ---
    private const val KEY_FORCE_LOCAL_NOTIFICATIONS = "force_local_notifications"

    fun isForceLocalNotificationsEnabled(ctx: Context): Boolean {
        val names = LEGACY_NAMES
        for (n in names) {
            try {
                val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
                if (p.contains(KEY_FORCE_LOCAL_NOTIFICATIONS)) return p.getBoolean(KEY_FORCE_LOCAL_NOTIFICATIONS, false)
            } catch (_: Exception) { }
        }
        return false
    }

    fun setForceLocalNotificationsEnabled(ctx: Context, enabled: Boolean) {
        try {
            val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            p.edit().putBoolean(KEY_FORCE_LOCAL_NOTIFICATIONS, enabled).apply()
        } catch (_: Exception) { }

        try {
            val p2 = ctx.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
            p2.edit().putBoolean(KEY_FORCE_LOCAL_NOTIFICATIONS, enabled).apply()
        } catch (_: Exception) { }
    }
}
