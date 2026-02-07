package com.moxiang.deepwiki.core.ui.scripts

import java.util.UUID

enum class ScriptRunAt(val value: String) {
    DOCUMENT_START("document_start"),
    DOCUMENT_END("document_end");

    companion object {
        fun fromValue(value: String?): ScriptRunAt {
            return when (value?.lowercase()) {
                DOCUMENT_START.value -> DOCUMENT_START
                DOCUMENT_END.value -> DOCUMENT_END
                else -> DOCUMENT_END
            }
        }
    }
}

const val BuiltinReaderScriptId = "builtin_reader_enhancer"

data class UserScript(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val matchPatterns: List<String>,
    val enabled: Boolean = true,
    val runAt: ScriptRunAt = ScriptRunAt.DOCUMENT_END
)

fun createBuiltinReaderScript(
    name: String,
    content: String,
    enabled: Boolean
): UserScript {
    return UserScript(
        id = BuiltinReaderScriptId,
        name = name,
        content = content,
        matchPatterns = listOf("*://*/*"),
        enabled = enabled,
        runAt = ScriptRunAt.DOCUMENT_END
    )
}

fun UserScript.matchesUrl(url: String): Boolean {
    if (!enabled) return false
    if (matchPatterns.isEmpty()) return true
    return matchPatterns.any { pattern -> patternMatchesUrl(pattern, url) }
}

private fun patternMatchesUrl(pattern: String, url: String): Boolean {
    val trimmed = pattern.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed == "*") return true
    if (trimmed.startsWith("regex:", ignoreCase = true)) {
        val regex = trimmed.substringAfter("regex:", "")
        if (regex.isBlank()) return false
        return runCatching { Regex(regex).containsMatchIn(url) }.getOrDefault(false)
    }
    if (!trimmed.contains("*")) {
        return trimmed == url
    }
    val regex = trimmed
        .split("*")
        .joinToString(".*") { part -> Regex.escape(part) }
    return Regex("^$regex$").matches(url)
}
