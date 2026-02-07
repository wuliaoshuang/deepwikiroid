package com.moxiang.deepwiki.feature.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moxiang.deepwiki.core.ui.theme.CodeBlockBackground
import com.moxiang.deepwiki.core.ui.theme.CodeBlockText
import com.moxiang.deepwiki.core.ui.theme.Spacing
import com.moxiang.deepwiki.core.ui.components.PressableIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    repositoryName: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFavorite by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = repositoryName,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    PressableIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    PressableIconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏"
                        )
                    }
                    PressableIconButton(onClick = { /* TODO: 分享 */ }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "分享"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(Spacing.Medium)
        ) {
            // 标题
            Text(
                text = "Visual Studio Code",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // 副标题
            Text(
                text = "Code editing. Redefined.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Spacing.Large))

            // 章节标题
            Text(
                text = "Features",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.Small))

            // 正文
            Text(
                text = "Visual Studio Code is a lightweight but powerful source code editor which runs on your desktop...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Spacing.Medium))

            // 代码块示例
            CodeBlock(
                code = """
                    import * as vscode from 'vscode';

                    export function activate() {
                        console.log('Extension loaded');
                    }
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun CodeBlock(
    code: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = CodeBlockBackground
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium,
            color = CodeBlockText,
            modifier = Modifier.padding(Spacing.Medium)
        )
    }
}
