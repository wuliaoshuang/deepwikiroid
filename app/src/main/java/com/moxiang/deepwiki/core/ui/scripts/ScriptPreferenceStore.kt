package com.moxiang.deepwiki.core.ui.scripts

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.scriptDataStore by preferencesDataStore(name = "script_prefs")
private val ScriptsKey = stringPreferencesKey("user_scripts_json")
private val BuiltinReaderEnabledKey = booleanPreferencesKey("builtin_reader_enabled")

class ScriptPreferenceStore(private val context: Context) {
    val scriptsFlow: Flow<List<UserScript>> = context.scriptDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            parseScriptsJson(prefs[ScriptsKey])
        }

    val builtinReaderEnabledFlow: Flow<Boolean> = context.scriptDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs ->
            prefs[BuiltinReaderEnabledKey] ?: true
        }

    suspend fun addScript(script: UserScript) {
        updateScripts { current ->
            current + script
        }
    }

    suspend fun updateScript(script: UserScript) {
        updateScripts { current ->
            current.map { if (it.id == script.id) script else it }
        }
    }

    suspend fun deleteScript(scriptId: String) {
        updateScripts { current ->
            current.filterNot { it.id == scriptId }
        }
    }

    suspend fun setScriptEnabled(scriptId: String, enabled: Boolean) {
        updateScripts { current ->
            current.map { if (it.id == scriptId) it.copy(enabled = enabled) else it }
        }
    }

    suspend fun setBuiltinReaderEnabled(enabled: Boolean) {
        context.scriptDataStore.edit { prefs ->
            prefs[BuiltinReaderEnabledKey] = enabled
        }
    }

    private suspend fun updateScripts(transform: (List<UserScript>) -> List<UserScript>) {
        context.scriptDataStore.edit { prefs ->
            val current = parseScriptsJson(prefs[ScriptsKey])
            val updated = transform(current)
            prefs[ScriptsKey] = scriptsToJson(updated)
        }
    }
}

private fun parseScriptsJson(raw: String?): List<UserScript> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(parseScript(obj))
            }
        }
    }.getOrDefault(emptyList())
}

private fun parseScript(obj: JSONObject): UserScript {
    val id = obj.optString("id").ifBlank { java.util.UUID.randomUUID().toString() }
    val name = obj.optString("name", "Untitled")
    val content = obj.optString("content", "")
    val matchArray = obj.optJSONArray("matches") ?: JSONArray()
    val matches = buildList {
        for (i in 0 until matchArray.length()) {
            val value = matchArray.optString(i).orEmpty().trim()
            if (value.isNotEmpty()) add(value)
        }
    }
    val enabled = obj.optBoolean("enabled", true)
    val runAt = ScriptRunAt.fromValue(obj.optString("runAt"))
    return UserScript(
        id = id,
        name = name,
        content = content,
        matchPatterns = matches,
        enabled = enabled,
        runAt = runAt
    )
}

private fun scriptsToJson(scripts: List<UserScript>): String {
    val array = JSONArray()
    scripts.forEach { script ->
        val obj = JSONObject()
        obj.put("id", script.id)
        obj.put("name", script.name)
        obj.put("content", script.content)
        obj.put("enabled", script.enabled)
        obj.put("runAt", script.runAt.value)
        val matches = JSONArray()
        script.matchPatterns.forEach { matches.put(it) }
        obj.put("matches", matches)
        array.put(obj)
    }
    return array.toString()
}
