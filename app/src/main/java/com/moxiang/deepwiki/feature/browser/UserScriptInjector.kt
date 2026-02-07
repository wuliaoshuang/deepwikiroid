package com.moxiang.deepwiki.feature.browser

import com.moxiang.deepwiki.core.ui.scripts.UserScript
internal fun buildDocumentEndBundle(scripts: List<UserScript>): String? {
    val active = scripts.filter { it.enabled && it.content.isNotBlank() }
    if (active.isEmpty()) return null
    val blocks = active.joinToString("\n") { buildUserScriptBlock(it) }
    return """
        (function() {
        $blocks
        })();
    """.trimIndent()
}

internal fun injectUserScriptsIntoHtml(
    html: String,
    startScripts: List<UserScript>,
    endScripts: List<UserScript>
): String {
    val startBlock = buildScriptTags(startScripts)
    val endBlock = buildScriptTags(endScripts)
    if (startBlock.isBlank() && endBlock.isBlank()) return html
    var result = html
    if (startBlock.isNotBlank()) {
        result = insertAfterHeadOpen(result, startBlock)
    }
    if (endBlock.isNotBlank()) {
        result = insertBeforeBodyClose(result, endBlock)
    }
    return result
}

private fun buildScriptTags(scripts: List<UserScript>): String {
    val active = scripts.filter { it.enabled && it.content.isNotBlank() }
    if (active.isEmpty()) return ""
    return active.joinToString("\n") { script ->
        val safeContent = escapeScriptForHtml(script.content)
        val block = buildUserScriptBlock(script, safeContent)
        """<script data-dw-script-id="${script.id}">$block</script>"""
    }
}

private fun buildUserScriptBlock(script: UserScript, contentOverride: String? = null): String {
    val safeName = script.name.replace("'", "\\'")
    val content = contentOverride ?: script.content
    return """
        try {
          window.__DW_USER_SCRIPT_REGISTRY = window.__DW_USER_SCRIPT_REGISTRY || {};
          if (window.__DW_USER_SCRIPT_REGISTRY['${script.id}']) {
            // already executed
          } else {
            window.__DW_USER_SCRIPT_REGISTRY['${script.id}'] = true;
            $content
          }
        } catch (e) {
          console.error('UserScript $safeName error', e);
        }
    """.trimIndent()
}

private fun escapeScriptForHtml(content: String): String {
    return content.replace("</script>", "<\\/script>", ignoreCase = true)
}

private fun insertAfterHeadOpen(html: String, block: String): String {
    val headOpen = Regex("(?i)<head[^>]*>").find(html)
    if (headOpen != null) {
        val index = headOpen.range.last + 1
        return html.substring(0, index) + "\n" + block + "\n" + html.substring(index)
    }
    val htmlOpen = Regex("(?i)<html[^>]*>").find(html)
    if (htmlOpen != null) {
        val index = htmlOpen.range.last + 1
        return html.substring(0, index) + "\n" + block + "\n" + html.substring(index)
    }
    return block + "\n" + html
}

private fun insertBeforeBodyClose(html: String, block: String): String {
    val bodyClose = Regex("(?i)</body>").find(html)
    if (bodyClose != null) {
        val index = bodyClose.range.first
        return html.substring(0, index) + "\n" + block + "\n" + html.substring(index)
    }
    val htmlClose = Regex("(?i)</html>").find(html)
    if (htmlClose != null) {
        val index = htmlClose.range.first
        return html.substring(0, index) + "\n" + block + "\n" + html.substring(index)
    }
    return html + "\n" + block
}
