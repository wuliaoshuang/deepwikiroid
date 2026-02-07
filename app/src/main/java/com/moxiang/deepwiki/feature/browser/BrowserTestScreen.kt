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
                title = pageTitle,
                url = currentUrl,
                onBack = { webViewRef.value?.goBack() },
                onForward = { webViewRef.value?.goForward() },
                onRefresh = { webViewRef.value?.reload() },
                onStop = { webViewRef.value?.stopLoading() },
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
