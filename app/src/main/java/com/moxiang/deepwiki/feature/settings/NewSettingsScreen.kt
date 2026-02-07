package com.moxiang.deepwiki.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.MainHeader
import com.moxiang.deepwiki.core.ui.components.PressableTextButton
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.locale.AppLanguage
import com.moxiang.deepwiki.core.ui.locale.LocalLanguageStore
import com.moxiang.deepwiki.core.ui.scripts.LocalScriptStore
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.core.ui.translation.LocalTranslationStore
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.collections.listOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings Screen - Based on Pencil Design
 * Shows app settings with sections for Appearance, Storage, and About
 */
@Composable
fun NewSettingsScreen(
    onNavigateToScripts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeStore = LocalThemeStore.current
    val themeMode by themeStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
    val languageStore = LocalLanguageStore.current
    val languageMode by languageStore.languageFlow.collectAsState(initial = AppLanguage.defaultForLocale())
    val scriptStore = LocalScriptStore.current
    val scripts by scriptStore.scriptsFlow.collectAsState(initial = emptyList())
    val builtinEnabled by scriptStore.builtinReaderEnabledFlow.collectAsState(initial = true)
    val translationStore = LocalTranslationStore.current
    val translationApiKey by translationStore.apiKeyFlow.collectAsState(initial = "")
    val translationTargetLang by translationStore.targetLanguageFlow.collectAsState(initial = "Chinese")
    val translationEnabled by translationStore.enabledFlow.collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTranslationDialog by remember { mutableStateOf(false) }
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarPadding = 106.dp
    val themeLabel = when (themeMode) {
        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
        ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
    }
    val languageLabel = when (languageMode) {
        AppLanguage.ENGLISH -> stringResource(id = R.string.language_english)
        AppLanguage.CHINESE -> stringResource(id = R.string.language_chinese)
    }
    val scriptTotal = scripts.size + 1
    val scriptEnabledCount = scripts.count { it.enabled } + if (builtinEnabled) 1 else 0
    val scriptLabel = stringResource(
        id = R.string.scripts_enabled_count,
        scriptEnabledCount,
        scriptTotal
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Header (Fixed at top, handles status bar spacing)
        MainHeader(
            title = "Settings",
            titleGradient = listOf(
                MaterialTheme.colorScheme.onBackground,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
        )

        // Content
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 20.dp + navBarsBottom + tabBarPadding)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Appearance Section
            item {
                SectionLabel(stringResource(id = R.string.settings_section_appearance))
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = stringResource(id = R.string.settings_theme),
                        value = themeLabel,
                        showChevron = true,
                        onClick = { showThemeDialog = true }
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.TextFields,
                        title = stringResource(id = R.string.settings_font_size),
                        value = stringResource(id = R.string.settings_font_medium),
                        showChevron = true,
                        onClick = { /* TODO */ }
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Language,
                        title = stringResource(id = R.string.settings_language),
                        value = languageLabel,
                        showChevron = true,
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Scripts Section
            item {
                SectionLabel(stringResource(id = R.string.settings_section_scripts))
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Extension,
                        title = stringResource(id = R.string.settings_scripts),
                        value = scriptLabel,
                        showChevron = true,
                        onClick = onNavigateToScripts
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Translation Section
            item {
                SectionLabel("Translation Settings")
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Translate,
                        title = "Translation",
                        value = if (translationEnabled) "Enabled" else "Disabled",
                        showChevron = true,
                        onClick = { showTranslationDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Storage Section
            item {
                SectionLabel(stringResource(id = R.string.settings_section_storage))
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Storage,
                        title = stringResource(id = R.string.settings_cache_size),
                        value = "128 MB",
                        showChevron = false,
                        onClick = {}
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Delete,
                        title = stringResource(id = R.string.settings_clear_cache),
                        value = null,
                        showChevron = false,
                        onClick = { /* TODO */ }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // About Section
            item {
                SectionLabel(stringResource(id = R.string.settings_section_about))
            }

            item {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = stringResource(id = R.string.settings_version),
                        value = "1.0.0",
                        showChevron = false,
                        onClick = {}
                    )
                    Divider()
                    SettingsRow(
                        icon = Icons.Default.Code,
                        title = stringResource(id = R.string.settings_github_repo),
                        value = null,
                        showChevron = false,
                        showExternalIcon = true,
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(stringResource(id = R.string.settings_theme)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeOptionRow(
                        label = stringResource(id = R.string.theme_system),
                        selected = themeMode == ThemeMode.SYSTEM,
                        onClick = {
                            scope.launch { themeStore.setThemeMode(ThemeMode.SYSTEM) }
                            showThemeDialog = false
                        }
                    )
                    ThemeOptionRow(
                        label = stringResource(id = R.string.theme_light),
                        selected = themeMode == ThemeMode.LIGHT,
                        onClick = {
                            scope.launch { themeStore.setThemeMode(ThemeMode.LIGHT) }
                            showThemeDialog = false
                        }
                    )
                    ThemeOptionRow(
                        label = stringResource(id = R.string.theme_dark),
                        selected = themeMode == ThemeMode.DARK,
                        onClick = {
                            scope.launch { themeStore.setThemeMode(ThemeMode.DARK) }
                            showThemeDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                PressableTextButton(onClick = { showThemeDialog = false }) {
                    Text(stringResource(id = R.string.settings_close))
                }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(id = R.string.settings_language_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeOptionRow(
                        label = stringResource(id = R.string.language_english),
                        selected = languageMode == AppLanguage.ENGLISH,
                        onClick = {
                            scope.launch {
                                languageStore.setLanguage(AppLanguage.ENGLISH)
                                val locales = LocaleListCompat.forLanguageTags(AppLanguage.ENGLISH.localeTag)
                                if (AppCompatDelegate.getApplicationLocales() != locales) {
                                    withContext(Dispatchers.Main) {
                                        AppCompatDelegate.setApplicationLocales(locales)
                                    }
                                }
                            }
                            showLanguageDialog = false
                        }
                    )
                    ThemeOptionRow(
                        label = stringResource(id = R.string.language_chinese),
                        selected = languageMode == AppLanguage.CHINESE,
                        onClick = {
                            scope.launch {
                                languageStore.setLanguage(AppLanguage.CHINESE)
                                val locales = LocaleListCompat.forLanguageTags(AppLanguage.CHINESE.localeTag)
                                if (AppCompatDelegate.getApplicationLocales() != locales) {
                                    withContext(Dispatchers.Main) {
                                        AppCompatDelegate.setApplicationLocales(locales)
                                    }
                                }
                            }
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                PressableTextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(id = R.string.settings_close))
                }
            }
        )
    }

    if (showTranslationDialog) {
        TranslationSettingsDialog(
            apiKey = translationApiKey,
            targetLanguage = translationTargetLang,
            enabled = translationEnabled,
            onDismiss = { showTranslationDialog = false },
            onSave = { newApiKey, newTargetLang, newEnabled ->
                scope.launch {
                    translationStore.setApiKey(newApiKey)
                    translationStore.setTargetLanguage(newTargetLang)
                    translationStore.setEnabled(newEnabled)
                }
                showTranslationDialog = false
            }
        )
    }
}

@Composable
private fun TranslationSettingsDialog(
    apiKey: String,
    targetLanguage: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean) -> Unit
) {
    var editApiKey by remember { mutableStateOf(apiKey) }
    var editTargetLang by remember { mutableStateOf(targetLanguage) }
    var editEnabled by remember { mutableStateOf(enabled) }
    var showApiKey by remember { mutableStateOf(false) }
    var expandedLang by remember { mutableStateOf(false) }

    val languages = listOf(
        "Chinese" to "中文",
        "English" to "English",
        "Japanese" to "日本語",
        "Korean" to "한국어",
        "French" to "Français",
        "German" to "Deutsch",
        "Spanish" to "Español"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Translation Settings") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Translation", fontSize = 15.sp)
                    Switch(
                        checked = editEnabled,
                        onCheckedChange = { editEnabled = it }
                    )
                }

                // API Key Input
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "DeepSeek API Key",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editApiKey,
                        onValueChange = { editApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showApiKey) "Hide API Key" else "Show API Key"
                                )
                            }
                        },
                        placeholder = { Text("sk-...") },
                        singleLine = true
                    )
                }

                // Target Language Dropdown
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Target Language",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box {
                        OutlinedTextField(
                            value = languages.find { it.first == editTargetLang }?.second ?: editTargetLang,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressable(onClick = { expandedLang = true }),
                            readOnly = true,
                            enabled = false,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select language"
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = expandedLang,
                            onDismissRequest = { expandedLang = false }
                        ) {
                            languages.forEach { (code, displayName) ->
                                DropdownMenuItem(
                                    text = { Text(displayName) },
                                    onClick = {
                                        editTargetLang = code
                                        expandedLang = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Help text
                Text(
                    "Get your API key from https://platform.deepseek.com",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            PressableTextButton(
                onClick = { onSave(editApiKey, editTargetLang, editEnabled) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            PressableTextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String?,
    showChevron: Boolean,
    showExternalIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.98f)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(id = R.string.cd_navigate),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (showExternalIcon) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = stringResource(id = R.string.cd_external),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick, pressedScale = 0.99f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label)
    }
}
