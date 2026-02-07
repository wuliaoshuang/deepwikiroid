package com.moxiang.deepwiki.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.NavHeader
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.components.PressableTextButton
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.scripts.LocalScriptStore
import com.moxiang.deepwiki.core.ui.scripts.BuiltinReaderScriptId
import com.moxiang.deepwiki.core.ui.scripts.ScriptRunAt
import com.moxiang.deepwiki.core.ui.scripts.UserScript
import com.moxiang.deepwiki.core.ui.scripts.createBuiltinReaderScript
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun ScriptManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scriptStore = LocalScriptStore.current
    val scripts by scriptStore.scriptsFlow.collectAsState(initial = emptyList())
    val builtinEnabled by scriptStore.builtinReaderEnabledFlow.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    var editingScript by remember { mutableStateOf<UserScript?>(null) }
    var pendingDelete by remember { mutableStateOf<UserScript?>(null) }
    val navBarsBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val builtinName = stringResource(id = R.string.scripts_reader_plugin)
    val displayScripts = remember(scripts, builtinEnabled, builtinName) {
        buildList {
            add(createBuiltinReaderScript(builtinName, "", builtinEnabled))
            addAll(scripts)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NavHeader(
            title = stringResource(id = R.string.scripts_title),
            onBackClick = onNavigateBack,
            actions = {
                PressableIconButton(onClick = { editingScript = UserScript("", "", "", listOf("*://*/*")) }) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.cd_add),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 16.dp + navBarsBottom
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (scripts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.scripts_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            items(displayScripts, key = { it.id }) { script ->
                val isBuiltin = script.id == BuiltinReaderScriptId
                ScriptRow(
                    script = script,
                    badge = if (isBuiltin) stringResource(id = R.string.scripts_builtin) else null,
                    onToggle = { enabled ->
                        scope.launch {
                            if (isBuiltin) {
                                scriptStore.setBuiltinReaderEnabled(enabled)
                            } else {
                                scriptStore.setScriptEnabled(script.id, enabled)
                            }
                        }
                    },
                    onEdit = if (isBuiltin) null else ({ editingScript = script }),
                    onDelete = if (isBuiltin) null else ({ pendingDelete = script })
                )
            }
        }
    }

    if (editingScript != null) {
        ScriptEditorDialog(
            script = editingScript,
            onDismiss = { editingScript = null },
            onSave = { saved ->
                scope.launch {
                    if (scripts.any { it.id == saved.id }) {
                        scriptStore.updateScript(saved)
                    } else {
                        scriptStore.addScript(saved)
                    }
                }
                editingScript = null
            }
        )
    }

    if (pendingDelete != null) {
        val script = pendingDelete!!
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(id = R.string.scripts_delete_title)) },
            text = { Text(stringResource(id = R.string.scripts_delete_message, script.name)) },
            confirmButton = {
                PressableTextButton(onClick = {
                    scope.launch { scriptStore.deleteScript(script.id) }
                    pendingDelete = null
                }) {
                    Text(stringResource(id = R.string.scripts_delete_confirm))
                }
            },
            dismissButton = {
                PressableTextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(id = R.string.scripts_cancel))
                }
            }
        )
    }
}

@Composable
private fun ScriptRow(
    script: UserScript,
    badge: String?,
    onToggle: (Boolean) -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val runAtLabel = when (script.runAt) {
        ScriptRunAt.DOCUMENT_START -> stringResource(id = R.string.scripts_run_at_start)
        ScriptRunAt.DOCUMENT_END -> stringResource(id = R.string.scripts_run_at_end)
    }
    val metaLine = if (!badge.isNullOrBlank()) "$runAtLabel · $badge" else runAtLabel
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(18.dp))
            .then(
                if (onEdit != null) {
                    Modifier.pressable(onClick = onEdit, pressedScale = 0.98f)
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name.ifBlank { stringResource(id = R.string.scripts_untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = metaLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = script.enabled,
                onCheckedChange = onToggle
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = script.matchPatterns.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (onEdit != null || onDelete != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEdit != null) {
                    PressableIconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(id = R.string.cd_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (onDelete != null) {
                    PressableIconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.cd_delete),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptEditorDialog(
    script: UserScript?,
    onDismiss: () -> Unit,
    onSave: (UserScript) -> Unit
) {
    val isEditing = script != null && script.id.isNotBlank()
    var name by remember(script?.id) { mutableStateOf(script?.name.orEmpty()) }
    var matches by remember(script?.id) { mutableStateOf(script?.matchPatterns?.joinToString("\n").orEmpty()) }
    var content by remember(script?.id) { mutableStateOf(script?.content.orEmpty()) }
    var enabled by remember(script?.id) { mutableStateOf(script?.enabled ?: true) }
    var runAt by remember(script?.id) { mutableStateOf(script?.runAt ?: ScriptRunAt.DOCUMENT_END) }
    var runAtExpanded by remember { mutableStateOf(false) }
    val untitledLabel = stringResource(id = R.string.scripts_untitled)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isEditing) stringResource(id = R.string.scripts_edit_title) else stringResource(id = R.string.scripts_add_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(id = R.string.scripts_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matches,
                    onValueChange = { matches = it },
                    label = { Text(stringResource(id = R.string.scripts_matches)) },
                    supportingText = { Text(stringResource(id = R.string.scripts_matches_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(stringResource(id = R.string.scripts_content)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    maxLines = 12
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.scripts_run_at),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Box {
                        OutlinedButton(onClick = { runAtExpanded = true }) {
                            Text(
                                text = when (runAt) {
                                    ScriptRunAt.DOCUMENT_START -> stringResource(id = R.string.scripts_run_at_start)
                                    ScriptRunAt.DOCUMENT_END -> stringResource(id = R.string.scripts_run_at_end)
                                }
                            )
                        }
                        DropdownMenu(
                            expanded = runAtExpanded,
                            onDismissRequest = { runAtExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.scripts_run_at_start)) },
                                onClick = {
                                    runAt = ScriptRunAt.DOCUMENT_START
                                    runAtExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.scripts_run_at_end)) },
                                onClick = {
                                    runAt = ScriptRunAt.DOCUMENT_END
                                    runAtExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.scripts_enabled))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
            }
        },
        confirmButton = {
            PressableTextButton(onClick = {
                val finalName = name.trim().ifBlank { untitledLabel }
                val matchPatterns = normalizeMatchInput(matches)
                val id = if (isEditing) script!!.id else UUID.randomUUID().toString()
                onSave(
                    UserScript(
                        id = id,
                        name = finalName,
                        content = content.trim(),
                        matchPatterns = matchPatterns,
                        enabled = enabled,
                        runAt = runAt
                    )
                )
            }) {
                Text(stringResource(id = R.string.scripts_save))
            }
        },
        dismissButton = {
            PressableTextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.scripts_cancel))
            }
        }
    )
}

private fun normalizeMatchInput(text: String): List<String> {
    val patterns = text.split("\n", ",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return if (patterns.isEmpty()) listOf("*://*/*") else patterns
}
