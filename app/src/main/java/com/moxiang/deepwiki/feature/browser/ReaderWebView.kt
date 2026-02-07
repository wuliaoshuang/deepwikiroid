package com.moxiang.deepwiki.feature.browser

import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.scripts.LocalScriptStore
import com.moxiang.deepwiki.core.ui.scripts.ScriptRunAt
import com.moxiang.deepwiki.core.ui.scripts.UserScript
import com.moxiang.deepwiki.core.ui.scripts.createBuiltinReaderScript
import com.moxiang.deepwiki.core.ui.scripts.matchesUrl
import java.io.ByteArrayInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import okhttp3.OkHttpClient
import okhttp3.Request

@Composable
fun ReaderWebView(
    url: String,
    modifier: Modifier = Modifier,
    webViewRef: MutableState<WebView?>? = null,
    userAgent: String? = null,
    onTitle: (String) -> Unit = {},
    onUrlChanged: (String) -> Unit = {},
    onLoading: (Boolean) -> Unit = {},
    onProgress: (Int) -> Unit = {},
    onNavigationState: (canGoBack: Boolean, canGoForward: Boolean) -> Unit = { _, _ -> },
    onDefaultUserAgent: (String) -> Unit = {},
    onScroll: (Int) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val scriptStore = LocalScriptStore.current
    val scripts by scriptStore.scriptsFlow.collectAsState(initial = emptyList())
    val builtinEnabled by scriptStore.builtinReaderEnabledFlow.collectAsState(initial = true)
    val scriptSnapshot = remember { AtomicReference<List<UserScript>>(emptyList()) }
    val injectionInfo = remember { AtomicReference(ScriptInjectionInfo(null, false)) }
    val okHttpClient = remember { OkHttpClient() }
    val builtinContent = remember(isDark) { buildReaderInjectionJs(isDark) }
    val builtinScriptName = stringResource(id = R.string.scripts_reader_plugin)
    val activeScripts = remember(scripts, builtinEnabled, builtinContent, builtinScriptName) {
        buildList {
            if (builtinEnabled) {
                add(createBuiltinReaderScript(builtinScriptName, builtinContent, true))
            }
            addAll(scripts)
        }
    }

    LaunchedEffect(activeScripts) {
        scriptSnapshot.set(activeScripts)
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                webViewRef?.value = this
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                val defaultUa = settings.userAgentString
                onDefaultUserAgent(defaultUa)
                if (!userAgent.isNullOrBlank()) {
                    settings.userAgentString = userAgent
                }
                isVerticalScrollBarEnabled = true
                isHorizontalScrollBarEnabled = false
                visibility = View.INVISIBLE

                setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
                    onScroll(scrollY - oldScrollY)
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        val safe = title?.trim().orEmpty()
                        if (safe.isNotEmpty()) {
                            onTitle(safe)
                        }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        onProgress(newProgress.coerceIn(0, 100))
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        if (!request.isForMainFrame) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        if (!request.method.equals("GET", ignoreCase = true)) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        val url = request.url?.toString().orEmpty()
                        if (!url.startsWith("http")) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        val matched = scriptSnapshot.get()
                            .filter { it.matchesUrl(url) }
                        if (matched.isEmpty()) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        Log.d("UserScripts", "Intercepting main document for $url (scripts=${matched.size})")
                        val startScripts = matched.filter { it.runAt == ScriptRunAt.DOCUMENT_START }
                        val endScripts = matched.filter { it.runAt == ScriptRunAt.DOCUMENT_END }
                        val response = fetchMainHtml(view, request, okHttpClient) ?: return super.shouldInterceptRequest(view, request)
                        if (!response.mimeType.contains("text/html", ignoreCase = true)) {
                            return super.shouldInterceptRequest(view, request)
                        }
                        val injected = injectUserScriptsIntoHtml(response.html, startScripts, endScripts)
                        injectionInfo.set(ScriptInjectionInfo(url, true))
                        return WebResourceResponse(
                            response.mimeType,
                            response.charset.name(),
                            ByteArrayInputStream(injected.toByteArray(response.charset))
                        )
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        onLoading(true)
                        val effectiveUrl = view?.url ?: url
                        effectiveUrl?.let { onUrlChanged(it) }
                        injectionInfo.set(ScriptInjectionInfo(effectiveUrl, false))
                    }

                    override fun onPageFinished(view: WebView, url: String) {
                        val effectiveUrl = view.url ?: url
                        view.visibility = View.VISIBLE
                        val matched = scriptSnapshot.get()
                            .filter { it.matchesUrl(effectiveUrl) }
                        if (matched.isNotEmpty()) {
                            Log.d("UserScripts", "onPageFinished matched=${matched.size} url=$effectiveUrl")
                        } else {
                            Log.d("UserScripts", "onPageFinished matched=0 url=$effectiveUrl")
                        }
                        val bundle = buildDocumentEndBundle(matched)
                        if (!bundle.isNullOrBlank()) {
                            view.evaluateJavascript(
                                "console.log('DW user scripts executing: ${matched.size}');",
                                null
                            )
                            view.evaluateJavascript(bundle, null)
                        }
                        onLoading(false)
                        onUrlChanged(effectiveUrl)
                        onNavigationState(view.canGoBack(), view.canGoForward())
                    }
                }
            }
        },
        update = { webView ->
            val tag = (webView.tag as? WebViewTag) ?: WebViewTag(null, null)
            if (!userAgent.isNullOrBlank() && tag.ua != userAgent) {
                webView.settings.userAgentString = userAgent
                tag.ua = userAgent
                webView.reload()
            }
            if (tag.url != url) {
                tag.url = url
                injectionInfo.set(ScriptInjectionInfo(url, false))
                webView.loadUrl(url)
            }
            webView.tag = tag
        }
    )
}

private data class WebViewTag(
    var url: String?,
    var ua: String?
)

private data class ScriptInjectionInfo(
    val url: String?,
    val intercepted: Boolean
)

private data class HtmlResponse(
    val html: String,
    val mimeType: String,
    val charset: Charset
)

private fun fetchMainHtml(
    view: WebView,
    request: WebResourceRequest,
    client: OkHttpClient
): HtmlResponse? {
    return runCatching {
        val url = request.url?.toString().orEmpty()
        val builder = Request.Builder().url(url)
        request.requestHeaders.forEach { (key, value) ->
            if (!key.equals("Accept-Encoding", ignoreCase = true)) {
                builder.addHeader(key, value)
            }
        }
        builder.header("User-Agent", view.settings.userAgentString)
        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrBlank()) {
            builder.header("Cookie", cookies)
        }
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body ?: return null
            val mediaType = body.contentType()
            val mimeType = mediaType?.let { "${it.type}/${it.subtype}" }
                ?: response.header("Content-Type")?.substringBefore(";")?.trim()
                ?: "text/html"
            val charset = mediaType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val html = body.string()
            HtmlResponse(html, mimeType, charset)
        }
    }.getOrNull()
}

private fun buildReaderInjectionJs(isDark: Boolean): String {
    val themeValue = if (isDark) "dark" else "light"
    return """
        (function() {
        
        // 1. 定义回调函数
        const callback = (mutationsList, observer) => {
          for (const mutation of mutationsList) {
            if (mutation.type === 'childList') {
              mutation.addedNodes.forEach((node) => {
                // 确保是元素节点 (Node.ELEMENT_NODE = 1)
                if (node.nodeType === 1) {
                  // 检查节点本身是否匹配
                  if (node.matches('[role*="dialog"]')) {
                    console.log('检测到 Overlay 出现 (直接节点):', node);
                    // 在这里执行你的逻辑...
                  } 
                  // 检查节点内部是否包含目标 (防止 overlay 包裹在其他 div 中一起插入)
                  else if (node.querySelector) {
                    const overlay = node.querySelector('[role*="dialog"]');
                    if (overlay) {
                      console.log('检测到 Overlay 出现 (子节点):', overlay);
                      // 在这里执行你的逻辑...
                      document.querySelectorAll('[role*="dialog"]').forEach(function(el) {
              el.style.minHeight = '350px';
              el.style.maxHeight = '1000px';
            });
                    }
                  }
                }
              });
            }
          }
        };

        // 2. 创建观察者实例
        const observer = new MutationObserver(callback);

        // 3. 开始观察 document.body (因为 dialog 通常直接挂载在 body 下)
        observer.observe(document.body, {
          childList: true, // 监听子节点的增删
          subtree: true    // 监听所有后代节点（不仅是直接子节点）
        });

        // 4. (可选) 当你不再需要监听时停止观察
        // observer.disconnect();
        
          function applyThemeClass() {
            ((a,b,c,d,e,f,g,h)=>{let i=document.documentElement,j=["light","dark"];function k(b){var c;(Array.isArray(a)?a:[a]).forEach(a=>{let c="class"===a,d=c&&f?e.map(a=>f[a]||a):e;c?(i.classList.remove(...d),i.classList.add(f&&f[b]?f[b]:b)):i.setAttribute(a,b)}),c=b,h&&j.includes(c)&&(i.style.colorScheme=c)}if(d)k(d);else try{let a=localStorage.getItem(b)||c,d=g&&"system"===a?window.matchMedia("(prefers-color-scheme: dark)").matches?"dark":"light":a;k(d)}catch(a){}})("class","theme","light","$themeValue",["light","dark"],null,true,true)
          }

          function normalizeImages(root) {
            root.querySelectorAll('img').forEach(function(img) {
              if (!img.getAttribute('src') || img.getAttribute('src') === '') {
                var data = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original');
                if (data) img.setAttribute('src', data);
              }
            });
          }

          function removeBySelector(selector) {
            document.querySelectorAll(selector).forEach(function(el) { el.remove(); });
          }

          function removeFirst(selector) {
            var el = document.querySelector(selector);
            if (el) el.remove();
          }

          function fixDialogTrigger() {
          
          document.querySelectorAll('pre').forEach(function(el) {
              el.style.minHeight = '350px';
              el.style.maxHeight = '1000px';
            });
            document.querySelectorAll('[data-slot*="dialog-trigger"]').forEach(function(el) {
              el.style.minHeight = '350px';
              el.style.maxHeight = '1000px';
            });
            document.querySelectorAll('[role*="dialog"]').forEach(function(el) {
              el.style.minHeight = '350px';
              el.style.maxHeight = '1000px';
            });
          }

          function score(el) {
            var text = (el.innerText || '').trim();
            if (text.length < 200) return 0;
            var p = el.querySelectorAll('p').length;
            var pre = el.querySelectorAll('pre').length;
            var code = el.querySelectorAll('code').length;
            var h = el.querySelectorAll('h1,h2,h3').length;
            var img = el.querySelectorAll('img').length;
            var links = el.querySelectorAll('a').length;
            return text.length + p * 80 + pre * 160 + code * 20 + h * 120 + img * 40 - links * 3;
          }

          function pickMain() {
            var selectors = [
              'main', 'article', '[role=main]', '#content', '.content', '.markdown-body', '.prose',
              '.markdown', '[class*="markdown" i]', '[class*="content" i]', '[class*="article" i]'
            ];
            var candidates = [];
            selectors.forEach(function(sel) {
              document.querySelectorAll(sel).forEach(function(el) { candidates.push(el); });
            });
            if (candidates.length === 0) {
              document.querySelectorAll('main,article,section,div').forEach(function(el) { candidates.push(el); });
            }
            var best = null;
            var bestScore = 0;
            candidates.forEach(function(el) {
              var s = score(el);
              if (s > bestScore) { bestScore = s; best = el; }
            });
            return best;
          }

          function cleanupChrome() {
            removeBySelector('header, nav, footer, aside, [role="navigation"], [aria-label*="menu" i], [aria-label*="toc" i], [aria-label*="table of contents" i]');
            removeBySelector('[class*="sidebar" i], [class*="toc" i], [class*="nav" i], [class*="menu" i], [class*="header" i], [class*="footer" i]');
          }

          function run() {
            var done = document.documentElement.getAttribute('data-dw-reader') === '1';
            var main = pickMain();
            if (main && (main.innerText || '').trim().length > 200) {
              var keep = main;
              while (keep.parentElement && keep.parentElement !== document.body) {
                keep = keep.parentElement;
              }
              Array.prototype.slice.call(document.body.children).forEach(function(child) {
                if (child !== keep) {
                  child.remove();
                }
              });
              normalizeImages(keep);
              document.documentElement.setAttribute('data-dw-reader', '1');
            } else if (done) {
              return;
            }
            applyThemeClass();
           
            removeFirst('.font-geist-mono');
            removeFirst('[class*="container mx-auto flex w-full flex-row items-center gap-2 py-4 md:py-6"]');
            removeFirst('[class*="flex cursor-pointer items-center"]');
            removeFirst('[class*="pointer-events-none fixed bottom-2 left-2 right-2 mt-2 md:bottom-4 md:left-0 md:right-0"]');
            cleanupChrome();
            fixDialogTrigger();
          }

          setTimeout(run, 1600);
        })();
    """.trimIndent()
}
