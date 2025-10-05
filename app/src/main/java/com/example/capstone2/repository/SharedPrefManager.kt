package com.example.capstone2.repository

import android.content.Context

object SharedPrefManager {
    private const val PREFS_NAME = "capstone_prefs"

    /**
     * Safely returns the stored userID from known preference files if present.
     * Accepts values stored as Long, Int, String (numeric), Float, Double.
     */
    fun getUserId(ctx: Context): Long? {
        val names = arrayOf("capstone_prefs", "MyAppPrefs")
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

    fun saveAuthToken(ctx: Context, token: String) {
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        p.edit().putString("auth_token", token).apply()
    }

    /**
     * Try to find the auth token in any of the known preference files.
     * This is defensive because some activities use "MyAppPrefs" while the
     * centralized SharedPrefManager historically used "capstone_prefs".
     */
    fun getAuthToken(ctx: Context): String? {
        val names = arrayOf(PREFS_NAME, "MyAppPrefs")
        for (n in names) {
            val p = ctx.getSharedPreferences(n, Context.MODE_PRIVATE)
            val t = p.getString("auth_token", null)
            if (!t.isNullOrBlank()) return t
        }
        return null
    }

    fun saveUserId(ctx: Context, userId: Long) {
        val p = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        p.edit().putLong("userID", userId).apply()
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

    private fun keyConv(partnerId: Long) = "preview_conv_${partnerId}"
    private fun keyMsg(partnerId: Long) = "preview_msg_${partnerId}"
    private fun keyAt(partnerId: Long) = "preview_at_${partnerId}"

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
}
