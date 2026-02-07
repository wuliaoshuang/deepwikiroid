package com.moxiang.deepwiki.feature.browser

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.BrowserControls
import com.moxiang.deepwiki.core.ui.components.MainHeader
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.theme.Purple500
import com.moxiang.deepwiki.core.ui.theme.Purple700
import com.moxiang.deepwiki.core.ui.translation.LocalTranslationStore
import com.moxiang.deepwiki.core.ui.translation.TranslationService
import com.moxiang.deepwiki.core.ui.utils.showToast
import kotlinx.coroutines.launch

@Composable
fun BrowserTestScreen(
    onNavigateBack: () -> Unit,
    url: String = "https://deepwiki.com/microsoft/vscode"
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    val bookmarks = remember { mutableStateListOf<String>() }
    var currentUrl by remember { mutableStateOf(url) }
    var pageTitle by remember { mutableStateOf("DeepWiki") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var defaultUa by remember { mutableStateOf<String?>(null) }
    var useAndroidUa by remember { mutableStateOf(true) }
    val linkCopiedText = stringResource(id = R.string.link_copied)

    // Translation state
    val translationStore = LocalTranslationStore.current
    val translationApiKey by translationStore.apiKeyFlow.collectAsState(initial = "")
    val translationTargetLang by translationStore.targetLanguageFlow.collectAsState(initial = "Chinese")
    val translationEnabled by translationStore.enabledFlow.collectAsState(initial = false)
    var isTranslating by remember { mutableStateOf(false) }
    var translationProgress by remember { mutableStateOf("0/0") }
    val translationService = remember { TranslationService() }
    val scope = rememberCoroutineScope()

    fun handleTranslate() {
        val webView = webViewRef.value ?: return

        if (translationApiKey.isBlank()) {
            context.showToast("请先在设置中配置 DeepSeek API Key")
            return
        }

        scope.launch {
            isTranslating = true
            try {
                translationService.translatePage(
                    webView = webView,
                    apiKey = translationApiKey,
                    targetLang = translationTargetLang,
                    onProgress = { current, total ->
                        translationProgress = "$current/$total"
                    }
                ).onFailure { error ->
                    // 只在失败时显示错误，不打断正常翻译
                    android.util.Log.e("Translation", "翻译失败", error)
                    context.showToast("翻译失败: ${error.message}")
                }
            } finally {
                isTranslating = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        MainHeader(
            title = "DeepWiki",
            subtitle = stringResource(id = R.string.home_subtitle),
            titleGradient = listOf(Purple500, Purple500.copy(0.7f)),
            leading = {
                PressableIconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Purple500,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
            ReaderWebView(
                url = currentUrl,
                modifier = Modifier.fillMaxSize(),
                webViewRef = webViewRef,
                userAgent = if (useAndroidUa) "android" else defaultUa,
                onTitle = { pageTitle = it },
                onUrlChanged = { currentUrl = it },
                onLoading = { isLoading = it },
                onProgress = { progress = it },
                onNavigationState = { back, forward ->
                    canGoBack = back
                    canGoForward = forward
                },
                onDefaultUserAgent = { ua -> defaultUa = ua }
            )

            BrowserControls(
                modifier = Modifier.align(Alignment.BottomCenter),
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isLoading = isLoading,
                isTranslating = isTranslating,
                translationEnabled = translationEnabled && translationApiKey.isNotBlank(),
                title = pageTitle,
                url = currentUrl,
                onBack = { webViewRef.value?.goBack() },
                onForward = { webViewRef.value?.goForward() },
                onRefresh = { webViewRef.value?.reload() },
                onStop = { webViewRef.value?.stopLoading() },
                onTranslate = { handleTranslate() },
                onCopy = {
                    clipboard.setText(AnnotatedString(currentUrl))
                    Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
                },
                onOpenExternal = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                        context.startActivity(intent)
                    }
                }
            )
        }
    }
}
