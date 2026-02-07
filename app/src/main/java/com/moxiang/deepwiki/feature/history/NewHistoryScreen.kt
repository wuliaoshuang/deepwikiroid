package com.moxiang.deepwiki.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.MainHeader
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.components.SwipeRevealItem
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.data.local.entity.HistoryEntity
import android.text.format.DateUtils
import java.util.Calendar

/**
 * History Screen - Based on Pencil Design
 * Matches specific design node PRFrs
 */
@Composable
fun NewHistoryScreen(
    onRepositoryClick: (HistoryEntity) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)
) {
    val history by viewModel.history.collectAsState()
    val grouped = remember(history) { groupHistory(history) }
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarPadding = 106.dp
    val canClear = history.isNotEmpty()
    val todayLabel = stringResource(id = R.string.history_today)
    val yesterdayLabel = stringResource(id = R.string.history_yesterday)
    val earlierLabel = stringResource(id = R.string.history_earlier)
    val emptyLabel = stringResource(id = R.string.history_empty)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Header: "Browse History" + "Clear All"
        MainHeader(
            title = "History",
            titleGradient = listOf(
                MaterialTheme.colorScheme.onBackground,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
            actions = {
                Text(
                    text = stringResource(id = R.string.history_clear_all),
                    modifier = Modifier.pressable(
                        onClick = { if (canClear) viewModel.clearAll() },
                        pressedScale = 0.97f
                    ),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (canClear) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

            if (grouped.today.isEmpty() && grouped.yesterday.isEmpty() && grouped.earlier.isEmpty()) {
                item {
                    Text(
                        text = emptyLabel,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                historySection(
                    label = todayLabel,
                    items = grouped.today,
                    onRepositoryClick = onRepositoryClick,
                    onDelete = { viewModel.removeHistory(it.repoName) }
                )
                historySection(
                    label = yesterdayLabel,
                    items = grouped.yesterday,
                    onRepositoryClick = onRepositoryClick,
                    onDelete = { viewModel.removeHistory(it.repoName) }
                )
                historySection(
                    label = earlierLabel,
                    items = grouped.earlier,
                    onRepositoryClick = onRepositoryClick,
                    onDelete = { viewModel.removeHistory(it.repoName) }
                )
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    repo: HistoryEntity,
    timeInfo: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // History Item Container (PRFrs)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(16.dp)
            )
            .pressable(onClick = onClick, pressedScale = 0.985f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon (2IfHo)
        Icon(
            imageVector = Icons.Default.Description, // Using Description as substitute for file-text
            contentDescription = null,
            tint = Purple500,
            modifier = Modifier.size(20.dp)
        )

        // Info Column (r3vTs)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = repo.repoName, // jCUkp
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = timeInfo, // 4xP8F
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    return DateUtils.getRelativeTimeSpanString(
        timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}

private fun groupHistory(list: List<HistoryEntity>): HistoryGroups {
    if (list.isEmpty()) return HistoryGroups(emptyList(), emptyList(), emptyList())
    val nowCal = Calendar.getInstance()
    val todayKey = dateKey(nowCal)
    val yesterdayCal = nowCal.clone() as Calendar
    yesterdayCal.add(Calendar.DATE, -1)
    val yesterdayKey = dateKey(yesterdayCal)
    val todayList = mutableListOf<HistoryEntity>()
    val yesterdayList = mutableListOf<HistoryEntity>()
    val earlierList = mutableListOf<HistoryEntity>()
    list.forEach { item ->
        val itemCal = Calendar.getInstance().apply { timeInMillis = item.lastVisited }
        val key = dateKey(itemCal)
        when (key) {
            todayKey -> todayList.add(item)
            yesterdayKey -> yesterdayList.add(item)
            else -> earlierList.add(item)
        }
    }
    return HistoryGroups(todayList, yesterdayList, earlierList)
}

private fun dateKey(calendar: Calendar): Int {
    val year = calendar.get(Calendar.YEAR)
    val day = calendar.get(Calendar.DAY_OF_YEAR)
    return year * 1000 + day
}

private data class HistoryGroups(
    val today: List<HistoryEntity>,
    val yesterday: List<HistoryEntity>,
    val earlier: List<HistoryEntity>
)

private fun LazyListScope.historySection(
    label: String,
    items: List<HistoryEntity>,
    onRepositoryClick: (HistoryEntity) -> Unit,
    onDelete: (HistoryEntity) -> Unit
) {
    if (items.isEmpty()) return
    item {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
    items(items, key = { it.repoName }) { repo ->
        SwipeRevealItem(
            modifier = Modifier.padding(horizontal = 20.dp),
            contentShape = RoundedCornerShape(16.dp),
            actionContent = {
                PressableIconButton(onClick = { onDelete(repo) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(id = R.string.cd_delete),
                        tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            content = {
                HistoryItemCard(
                    repo = repo,
                    timeInfo = formatRelativeTime(repo.lastVisited),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onRepositoryClick(repo) }
                )
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}
