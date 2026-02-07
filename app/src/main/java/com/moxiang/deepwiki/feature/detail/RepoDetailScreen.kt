package com.moxiang.deepwiki.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.BrowserControls
import com.moxiang.deepwiki.core.ui.components.MainHeader
import com.moxiang.deepwiki.core.ui.components.PressableButton
import com.moxiang.deepwiki.core.ui.components.PressableFloatingActionButton
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.data.model.WikiMenuDto
import com.moxiang.deepwiki.core.ui.translation.LocalTranslationStore
import com.moxiang.deepwiki.core.ui.translation.TranslationService
import com.moxiang.deepwiki.core.ui.utils.showToast
import com.moxiang.deepwiki.feature.browser.ReaderWebView
import kotlinx.coroutines.launch
import android.widget.Toast

/**
 * Repository Detail Screen
 * Shows repository documentation with tree-structured navigation menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repoName: String,
    repoDescription: String?,
    repoStars: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RepoDetailViewModel = viewModel(factory = RepoDetailViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var injectedTitle by remember { mutableStateOf<String?>(null) }
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val copiedText = stringResource(id = R.string.copied)
    val linkCopiedText = stringResource(id = R.string.link_copied)

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Translation state
    val translationStore = LocalTranslationStore.current
    val translationApiKey by translationStore.apiKeyFlow.collectAsState(initial = "")
    val translationTargetLang by translationStore.targetLanguageFlow.collectAsState(initial = "Chinese")
    val translationEnabled by translationStore.enabledFlow.collectAsState(initial = false)
    var isTranslating by remember { mutableStateOf(false) }
    var translationProgress by remember { mutableStateOf("0/0") }
    val translationService = remember { TranslationService() }

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
                    android.util.Log.e("Translation", "翻译失败", error)
                    context.showToast("翻译失败: ${error.message}")
                }
            } finally {
                isTranslating = false
            }
        }
    }

    LaunchedEffect(repoName) {
        viewModel.fetchDocument(repoName)
        val fallbackDescription = repoDescription?.takeIf { it.isNotBlank() }?.take(120).orEmpty()
        viewModel.recordHistory(repoName, fallbackDescription, repoStars)
    }

    val currentDocument = (uiState as? DetailUiState.Success)?.document
    LaunchedEffect(currentDocument?.htmlUrl) {
        injectedTitle = null
        // 切换文档时重置翻译状态
        isTranslating = false
        translationProgress = "0/0"
    }
    val displayTitle = injectedTitle ?: currentDocument?.title ?: repoName
    val displayMenuItems = currentDocument?.menuItems.orEmpty()
    val copyText = currentDocument?.content?.trim().orEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = navBarsBottom)
        ) {
            MainHeader(
                title = displayTitle,
                titleMaxLines = 1,
                leading = {
                    PressableIconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.cd_back),
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                actions = {
                    PressableIconButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(copyText))
                            Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                        },
                        enabled = copyText.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            stringResource(id = R.string.cd_copy_link),
                            tint = if (copyText.isNotEmpty()) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    PressableIconButton(
                        onClick = {
                            val fallbackDescription = currentDocument?.content?.take(100).orEmpty()
                            val favoriteDescription = repoDescription?.takeIf { it.isNotBlank() } ?: fallbackDescription
                            viewModel.toggleFavorite(favoriteDescription, repoStars)
                        }
                    ) {
                        Icon(
                            if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            stringResource(id = R.string.cd_favorite),
                            tint = if (isFavorite) Purple500 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            )

            when (val state = uiState) {
                is DetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple500)
                    }
                }
                is DetailUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(id = R.string.error_with_message, state.message), color = Color.Red)
                            PressableButton(
                                onClick = { viewModel.fetchDocument(repoName) },
                                modifier = Modifier.padding(top = 16.dp)
                            ) {
                                Text(stringResource(id = R.string.retry))
                            }
                        }
                    }
                }
                is DetailUiState.Success -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        ReaderWebView(
                            url = state.document.htmlUrl.ifBlank { "https://deepwiki.com" },
                            modifier = Modifier.fillMaxSize(),
                            webViewRef = webViewRef,
                            onTitle = { title ->
                                injectedTitle = title
                            },
                            onLoading = { isLoading = it },
                            onNavigationState = { back, forward ->
                                canGoBack = back
                                canGoForward = forward
                            },
                            onScroll = { dy ->
                                if (dy > 10 && controlsVisible) {
                                    controlsVisible = false
                                } else if (dy < -10 && !controlsVisible) {
                                    controlsVisible = true
                                }
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
        ) {
            BrowserControls(
                modifier = Modifier.navigationBarsPadding(),
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                isLoading = isLoading,
                title = displayTitle,
                url = currentDocument?.htmlUrl.orEmpty(),
                onBack = { webViewRef.value?.goBack() },
                onForward = { webViewRef.value?.goForward() },
                onRefresh = { webViewRef.value?.reload() },
                onStop = { webViewRef.value?.stopLoading() },
                onTranslate = { handleTranslate() },
                isTranslating = isTranslating,
                // 加载中禁用翻译，且必须配置了 Key
                translationEnabled = !isLoading && translationEnabled && translationApiKey.isNotBlank(),
                onCopy = {
                    clipboard.setText(AnnotatedString(currentDocument?.htmlUrl.orEmpty()))
                    Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
                },
                onOpenExternal = {
                    runCatching {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentDocument?.htmlUrl.orEmpty()))
                        context.startActivity(intent)
                    }
                }
            )
        }

        if (displayMenuItems.isNotEmpty()) {
            PressableFloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = Purple500,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = if (controlsVisible) 180.dp else 28.dp)
                    .navigationBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(id = R.string.cd_menu),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    // Tree-Structured Wiki Menu
    if (showBottomSheet && currentDocument != null) {
        ModalBottomSheet(
            modifier = Modifier.heightIn(),
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    stringResource(id = R.string.toc_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .padding(horizontal = 16.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(displayMenuItems.size) { index ->
                        WikiMenuTreeItem(
                            item = displayMenuItems[index],
                            level = 0,
                            onItemClick = { clickedItem ->
                                if (clickedItem.path.isNotEmpty()) {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                            if (clickedItem.path.startsWith("#")) {
                                                val id = clickedItem.path.removePrefix("#")
                                                webViewRef.value?.evaluateJavascript(
                                                    "document.getElementById('${'$'}id')?.scrollIntoView({behavior:'smooth', block:'start'});",
                                                    null
                                                )
                                            } else {
                                                viewModel.fetchDocument(clickedItem.path)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                if (displayMenuItems.isEmpty()) {
                    Text(
                        stringResource(id = R.string.toc_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WikiMenuTreeItem(
    item: WikiMenuDto,
    level: Int,
    onItemClick: (WikiMenuDto) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }
    val hasChildren = item.children.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (item.isSelected) Purple500.copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .pressable(pressedScale = 0.99f) {
                if (hasChildren && item.path.isEmpty()) {
                    isExpanded = !isExpanded
                } else {
                    onItemClick(item)
                }
            }
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .padding(start = (level * 16).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasChildren) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                contentDescription = if (isExpanded) {
                    stringResource(id = R.string.cd_collapse)
                } else {
                    stringResource(id = R.string.cd_expand)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .pressable(
                        onClick = { isExpanded = !isExpanded },
                        pressedScale = 0.9f,
                        pressedAlpha = 0.85f,
                        bounded = false
                    )
            )
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(24.dp))
        }

        if (!hasChildren) {
            Icon(
                imageVector = if (item.isSelected) Icons.Default.Description else Icons.Default.Article,
                contentDescription = null,
                tint = if (item.isSelected) Purple500 else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = item.title,
            fontSize = 15.sp,
            fontWeight = if (item.isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (item.isSelected) Purple500 else MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
        )
    }

    if (isExpanded && hasChildren) {
        item.children.forEach { child ->
            WikiMenuTreeItem(item = child, level = level + 1, onItemClick = onItemClick)
        }
    }
}
