package com.moxiang.deepwiki.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.moxiang.deepwiki.core.ui.components.PressableTextButton
import com.moxiang.deepwiki.core.ui.components.pressEffect
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 外观设置
            item {
                SettingsSectionHeader(title = "外观设置")
            }

            item {
                SettingsItem(
                    title = "主题模式",
                    subtitle = "跟随系统",
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsItem(
                    title = "字体大小",
                    subtitle = "中",
                    onClick = { /* TODO: 字体大小 */ }
                )
            }

            // 缓存管理
            item {
                SettingsSectionHeader(title = "缓存管理")
            }

            item {
                SettingsInfoItem(
                    title = "缓存大小",
                    value = "125 MB"
                )
            }

            item {
                SettingsItem(
                    title = "清空缓存",
                    onClick = { /* TODO: 清空缓存 */ }
                )
            }

            // 关于
            item {
                SettingsSectionHeader(title = "关于")
            }

            item {
                SettingsInfoItem(
                    title = "版本号",
                    value = "v1.0.0"
                )
            }

            item {
                SettingsItem(
                    title = "开源协议",
                    onClick = { /* TODO: 开源协议 */ }
                )
            }

            item {
                SettingsItem(
                    title = "GitHub仓库",
                    onClick = { /* TODO: GitHub */ }
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { /* TODO: 主题切换 */ }
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            horizontal = Spacing.Medium,
            vertical = Spacing.Small
        )
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .pressEffect(interactionSource),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.Medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Spacing.Medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (Int) -> Unit
) {
    var selectedTheme by remember { mutableStateOf(2) } // 0: 浅色, 1: 深色, 2: 跟随系统

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题模式") },
        text = {
            Column {
                ThemeOption(
                    text = "浅色",
                    selected = selectedTheme == 0,
                    onClick = { selectedTheme = 0 }
                )
                ThemeOption(
                    text = "深色",
                    selected = selectedTheme == 1,
                    onClick = { selectedTheme = 1 }
                )
                ThemeOption(
                    text = "跟随系统",
                    selected = selectedTheme == 2,
                    onClick = { selectedTheme = 2 }
                )
            }
        },
        confirmButton = {
            PressableTextButton(onClick = {
                onThemeSelected(selectedTheme)
                onDismiss()
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            PressableTextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun ThemeOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.99f)
            .padding(vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(text = text)
    }
}
