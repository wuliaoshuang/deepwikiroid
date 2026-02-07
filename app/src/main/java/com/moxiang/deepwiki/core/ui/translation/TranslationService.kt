package com.moxiang.deepwiki.core.ui.translation

import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * 文本块数据类
 */
data class TextBlock(
    val index: Int,
    val text: String
)

/**
 * 翻译服务
 * 负责从 WebView 提取内容、调用 DeepSeek API 翻译、并将结果注入回页面
 */
class TranslationService {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)  // 2 分钟超时，支持长文档
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * 提取页面内容的 JavaScript 代码
     * 优化：参考 Via 浏览器思路，直接遍历 TextNode，合并相邻文本，覆盖所有可见内容
     */
    private val extractContentJS = """
        (function() {
            const blocks = [];
            // 排除的标签
            const ignoreTags = new Set(['SCRIPT', 'STYLE', 'NOSCRIPT', 'IFRAME', 'OBJECT', 'EMBED', 'HEADER', 'NAV', 'FOOTER', 'ASIDE', 'SVG', 'IMG', 'CODE']);

            // 检查元素是否可见
            function isVisible(el) {
                if (!el) return false;
                const style = window.getComputedStyle(el);
                if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;
                const rect = el.getBoundingClientRect();
                return rect.width > 0 && rect.height > 0;
            }

            // 检查是否是应该忽略的容器
            function shouldIgnore(el) {
                if (!el) return false;
                if (ignoreTags.has(el.tagName)) return true;
                if (el.getAttribute('translate') === 'no') return true;
                if (el.classList.contains('notranslate')) return true;
                return shouldIgnore(el.parentElement); // 递归检查父元素
            }

            // 使用 TreeWalker 遍历所有文本节点 (NodeFilter.SHOW_TEXT)
            const walker = document.createTreeWalker(
                document.body,
                NodeFilter.SHOW_TEXT,
                {
                    acceptNode: function(node) {
                        if (!node.textContent.trim()) return NodeFilter.FILTER_REJECT;
                        if (shouldIgnore(node.parentElement)) return NodeFilter.FILTER_REJECT;
                        if (!isVisible(node.parentElement)) return NodeFilter.FILTER_REJECT;
                        return NodeFilter.FILTER_ACCEPT;
                    }
                }
            );

            let currentNode;
            let idx = 0;
            let currentBlock = null;

            // 安全转义函数：处理换行符、引号等特殊字符
            function safeText(str) {
                if (!str) return "";
                return str.replace(/\\/g, '\\\\')
                          .replace(/"/g, '\\"')
                          .replace(/\n/g, '\\n')
                          .replace(/\r/g, '\\r')
                          .replace(/\t/g, '\\t');
            }

            while (currentNode = walker.nextNode()) {
                const text = currentNode.textContent.trim();
                const parent = currentNode.parentElement;

                // 简单的启发式合并：如果父元素相同，或者是由 span 等内联元素分割的，可以视为同一段
                // 这里为了简单和准确，我们以 "块级容器" 为单位进行聚合

                // 找到最近的块级祖先
                let blockParent = parent;
                while (blockParent && window.getComputedStyle(blockParent).display.includes('inline') && blockParent !== document.body) {
                    blockParent = blockParent.parentElement;
                }

                if (!blockParent) blockParent = parent;

                // 检查这个块级元素是否已经处理过
                if (!blockParent.hasAttribute('data-translation-index')) {
                    // 如果没有处理过，提取该块级元素下的所有有效文本
                    // 注意：这里我们通过父元素来聚合，而不是单个 TextNode，这样能避免把一句话拆碎

                    // 再次检查该块级元素是否包含不需要翻译的内容（如代码块）
                    if (blockParent.tagName === 'PRE' || blockParent.tagName === 'CODE') continue;

                    // 提取该块下的纯文本（保留空格结构，但不包含隐藏元素）
                    const fullText = blockParent.innerText.trim(); // innerText 会自动忽略隐藏元素

                    if (fullText.length > 5) {
                        blockParent.setAttribute('data-translation-index', idx);
                        // 手动构建 JSON 对象字符串，避免 JSON.stringify 处理某些特殊字符时的兼容性问题
                        blocks.push('{"index":' + idx + ',"text":"' + safeText(fullText) + '"}');
                        idx++;
                    }
                }
            }

            return "[" + blocks.join(",") + "]";
        })();
    """.trimIndent()

    /**
     * 注入翻译结果的 JavaScript 代码模板
     */
    private fun getInjectTranslationJS(index: Int, translatedText: String): String {
        // 转义 JavaScript 字符串
        val safeText = translatedText
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        return """
            (function(index, translatedText) {
                const el = document.querySelector('[data-translation-index="' + index + '"]');
                if (el && !el.querySelector('.dw-translation')) {
                    const div = document.createElement('div');
                    div.className = 'dw-translation';
                    // Material 3 风格：大圆角、柔和背景、非斜体、淡入动画
                    div.style.cssText = 'margin-top:12px; padding:12px 16px; background-color:#fff; color:#191C20; border-radius:16px; font-size:0.95em; line-height:1.6; font-family:system-ui, -apple-system, sans-serif; opacity:0; transition:opacity 0.4s ease-out;';
                    div.textContent = translatedText;
                    el.appendChild(div);

                    // 触发淡入动画
                    requestAnimationFrame(() => {
                        div.style.opacity = '1';
                    });
                }
            })($index, '$safeText');
        """.trimIndent()
    }

    /**
     * 还原原文的 JavaScript 代码
     */
    private val restoreOriginalJS = """
        (function() {
            document.querySelectorAll('.dw-translation').forEach(el => el.remove());
            document.querySelectorAll('[data-translation-index]').forEach(el => {
                el.removeAttribute('data-translation-index');
            });
        })();
    """.trimIndent()

    /**
     * 清理翻译状态的 JavaScript 代码
     */
    private val clearTranslationStateJS = """
        (function() {
            document.querySelectorAll('.dw-translation').forEach(el => el.remove());
            document.querySelectorAll('[data-translation-index]').forEach(el => {
                el.removeAttribute('data-translation-index');
            });
        })();
    """.trimIndent()

    /**
     * 从 WebView 提取页面内容
     */
    suspend fun extractPageContent(webView: WebView): List<TextBlock> = withContext(Dispatchers.Main) {
        // 1. 先清理可能存在的旧状态，防止重复提取或残留
        webView.evaluateJavascript(clearTranslationStateJS, null)

        suspendCoroutine { continuation ->
            // 2. 提取新内容
            webView.evaluateJavascript(extractContentJS) { result ->
                try {
                    if (result == null || result == "null") {
                        continuation.resume(emptyList())
                        return@evaluateJavascript
                    }

                    // 移除首尾引号
                    val jsonString = result.trim().removeSurrounding("\"")
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")

                    val jsonArray = JSONArray(jsonString)
                    val blocks = mutableListOf<TextBlock>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        blocks.add(
                            TextBlock(
                                index = obj.getInt("index"),
                                text = obj.getString("text")
                            )
                        )
                    }

                    continuation.resume(blocks)
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    /**
     * 调用 DeepSeek API 翻译文本（支持超长文本）
     */
    suspend fun translateText(
        text: String,
        targetLang: String,
        apiKey: String
    ): String = withContext(Dispatchers.IO) {
        // 安全转义文本内容，避免 JSON 格式错误
        val safeText = text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val prompt = "Translate the following text to $targetLang. Preserve the paragraph separator '###DWSPLIT###' exactly as it appears. Only return the translation:\n\n$safeText"

        val json = JSONObject().apply {
            put("model", "deepseek-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.3)
            put("max_tokens", 8192)
        }

        val requestBody = json.toString()
        android.util.Log.d("TranslationAPI", "Request size: ${requestBody.length} chars")

        val request = Request.Builder()
            .url("https://api.deepseek.com/v1/chat/completions")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = okHttpClient.newCall(request).execute()

        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "No error body"
            android.util.Log.e("TranslationAPI", "API Error: ${response.code} - $errorBody")
            throw IOException("API Error ${response.code}: $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val responseJson = JSONObject(responseBody)

        responseJson.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
            .trim()
    }

    /**
     * 批量翻译页面 - 大批次分段翻译策略（速度最优）
     * @param webView WebView 实例
     * @param apiKey DeepSeek API Key
     * @param targetLang 目标语言
     * @param onProgress 进度回调 (current, total)
     * @return Result<Unit>
     */
    suspend fun translatePage(
        webView: WebView,
        apiKey: String,
        targetLang: String,
        onProgress: (Int, Int) -> Unit
    ): Result<Unit> = runCatching {
        // 1. 提取页面内容
        val textBlocks = extractPageContent(webView)

        if (textBlocks.isEmpty()) {
            throw IllegalStateException("未找到可翻译的内容")
        }

        // 2. 直接使用大批次分段翻译（速度最优）
        translatePageInBatches(webView, textBlocks, apiKey, targetLang, onProgress)
    }

    /**
     * 分批翻译策略 - 多线程并发，结果按顺序流式显示
     * 核心逻辑：并发启动所有请求 -> 按顺序 await 结果 -> 注入
     */
    private suspend fun translatePageInBatches(
        webView: WebView,
        textBlocks: List<TextBlock>,
        apiKey: String,
        targetLang: String,
        onProgress: (Int, Int) -> Unit
    ) = coroutineScope {
        val total = textBlocks.size
        val batchSize = 30  // 每批 30 段落
        val separator = "\n\n###DWSPLIT###\n\n"
        var completedCount = 0

        // 分批
        val batches = textBlocks.chunked(batchSize)

        // 1. 并发启动所有翻译任务 (不会阻塞)
        val deferredResults = batches.mapIndexed { batchIndex, batch ->
            async(Dispatchers.IO) {
                try {
                    val mergedText = batch.joinToString(separator) { it.text }
                    val mergedTranslation = translateText(mergedText, targetLang, apiKey)
                    val translations = mergedTranslation.split(separator)

                    batch.mapIndexed { idx, block ->
                        block.index to (translations.getOrNull(idx)?.trim() ?: "翻译失败")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TranslationBatch", "批次 $batchIndex 失败", e)
                    batch.map { it.index to "翻译失败: ${e.message}" }
                }
            }
        }

        // 2. 按顺序等待结果并注入 (保证显示顺序)
        // 即使后面的批次先下载完，也会在这里等待前面的批次处理完
        deferredResults.forEach { deferred ->
            val results = deferred.await()

            // 切换到主线程注入 UI
            withContext(Dispatchers.Main) {
                results.forEach { (index, translation) ->
                    webView.evaluateJavascript(
                        getInjectTranslationJS(index, translation),
                        null
                    )
                }

                completedCount += results.size
                onProgress(minOf(completedCount, total), total)
            }
        }
    }

    /**
     * 还原原文（移除所有翻译）
     */
    suspend fun restoreOriginal(webView: WebView) {
        withContext(Dispatchers.Main) {
            webView.evaluateJavascript(restoreOriginalJS, null)
        }
    }
}
