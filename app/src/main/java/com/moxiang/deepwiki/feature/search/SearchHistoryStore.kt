package com.moxiang.deepwiki.feature.search

import android.content.Context
import org.json.JSONArray

class SearchHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return try {
            val json = JSONArray(raw)
            buildList {
                for (i in 0 until json.length()) {
                    val value = json.optString(i).trim()
                    if (value.isNotEmpty()) add(value)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(list: List<String>) {
        val json = JSONArray()
        list.forEach { json.put(it) }
        prefs.edit().putString(KEY_RECENT_SEARCHES, json.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "search_history"
        const val KEY_RECENT_SEARCHES = "recent_searches"
    }
}
