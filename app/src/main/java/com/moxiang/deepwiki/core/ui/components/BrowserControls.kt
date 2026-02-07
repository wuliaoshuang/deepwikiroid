package com.moxiang.deepwiki.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moxiang.deepwiki.R

@Composable
fun BrowserControls(
    modifier: Modifier = Modifier,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    title: String,
    url: String,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onCopy: () -> Unit,
    onOpenExternal: () -> Unit,
    isTranslating: Boolean = false,
    translationEnabled: Boolean = false,
    onTranslate: () -> Unit = {}
) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // URL Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title.ifBlank { stringResource(id = R.string.loading) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrowserActionButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = stringResource(id = R.string.cd_back),
                    enabled = canGoBack,
                    onClick = onBack
                )

                BrowserActionButton(
                    icon = Icons.Default.ArrowForward,
                    contentDescription = stringResource(id = R.string.cd_forward),
                    enabled = canGoForward,
                    onClick = onForward
                )

                BrowserActionButton(
                    icon = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                    contentDescription = if (isLoading) {
                        stringResource(id = R.string.cd_stop)
                    } else {
                        stringResource(id = R.string.cd_refresh)
                    },
                    onClick = if (isLoading) onStop else onRefresh,
                    isPrimary = true
                )

                // Translation button (conditionally shown)
                if (translationEnabled) {
                    BrowserActionButton(
                        icon = if (isTranslating) Icons.Default.HourglassEmpty else Icons.Default.Translate,
                        contentDescription = "Translate",
                        enabled = !isTranslating,
                        onClick = onTranslate
                    )
                }

                BrowserActionButton(
                    icon = Icons.Default.ContentCopy,
                    contentDescription = stringResource(id = R.string.cd_copy_link),
                    onClick = onCopy
                )

                BrowserActionButton(
                    icon = Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(id = R.string.cd_open_in_browser),
                    onClick = onOpenExternal
                )
            }
        }
    }
}

@Composable
fun BrowserActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    if (isPrimary) {
        PressableFilledTonalIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        PressableIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
