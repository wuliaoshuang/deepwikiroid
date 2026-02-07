package com.moxiang.deepwiki.feature.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.EmptyView
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.components.PressableTextButton
import com.moxiang.deepwiki.core.ui.components.RepoCard
import com.moxiang.deepwiki.core.ui.components.SystemStatusBarSpacer
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.feature.favorites.FavoritesViewModel
import java.text.NumberFormat
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onRepositoryClick: (String, String?, Int) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SearchViewModel = viewModel(factory = SearchViewModelFactory(context))
    val favoritesViewModel: FavoritesViewModel =
        viewModel(factory = FavoritesViewModel.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val favorites by favoritesViewModel.favorites.collectAsState()
    val favoriteNames = remember(favorites) { favorites.map { it.repoName }.toSet() }
    val focusManager = LocalFocusManager.current
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listContentPadding = PaddingValues(
        start = 20.dp,
        top = 0.dp,
        end = 20.dp,
        bottom = navBarsBottom
    )
    val resultsListState = rememberLazyListState()
    val recentListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val activeListState = if (uiState.query.isBlank()) recentListState else resultsListState
    val showBackToTop by derivedStateOf {
        if (uiState.isSearching || uiState.errorDetail != null) {
            false
        } else {
            val state = if (uiState.query.isBlank()) recentListState else resultsListState
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 200
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SystemStatusBarSpacer()

        SearchRow(
            query = uiState.query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = {
                focusManager.clearFocus()
                viewModel.onSearchTriggered()
            },
            onClear = { viewModel.onQueryChange("") },
            onBack = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isSearching -> {
                    CircularProgressIndicator(
                        color = Purple500,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.errorDetail != null -> {
                    val detail = uiState.errorDetail.orEmpty()
                    val message = if (detail.isBlank()) {
                        stringResource(id = R.string.search_failed)
                    } else {
                        stringResource(id = R.string.search_failed_with_detail, detail)
                    }
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(Spacing.Medium)
                    )
                }
                uiState.query.isBlank() -> {
                    RecentSearchesSection(
                        recentSearches = uiState.recentSearches,
                        onClearAll = viewModel::clearRecentSearches,
                        onRemove = viewModel::removeRecentSearch,
                        onSelect = viewModel::onRecentSearchSelected,
                        listState = recentListState,
                        contentPadding = listContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    ResultsSection(
                        results = uiState.searchResults,
                        favoriteNames = favoriteNames,
                        onRepositoryClick = onRepositoryClick,
                        onToggleFavorite = { repo, isFavorite ->
                            if (isFavorite) {
                                favoritesViewModel.removeFavorite(repo.fullName)
                            } else {
                                favoritesViewModel.addFavorite(repo.fullName, repo.description, repo.stars)
                            }
                        },
                        listState = resultsListState,
                        contentPadding = listContentPadding,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showBackToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp, bottom = 28.dp)
                        .navigationBarsPadding()
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .pressable(
                            onClick = { scope.launch { activeListState.animateScrollToItem(0) } },
                            pressedScale = 0.92f,
                            pressedAlpha = 0.9f
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(id = R.string.cd_back_to_top),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PressableIconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(id = R.string.search_hint), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(id = R.string.cd_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    PressableIconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(id = R.string.cd_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier
                .height(52.dp)
                .weight(1f),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
        )
    }
}

@Composable
private fun ResultsSection(
    results: List<RepoUiModel>,
    favoriteNames: Set<String>,
    onRepositoryClick: (String, String?, Int) -> Unit,
    onToggleFavorite: (RepoUiModel, Boolean) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val countText = NumberFormat.getNumberInstance().format(results.size)

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        state = listState
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.search_results),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.search_results_count, countText),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (results.isEmpty()) {
            item {
                Text(
                    text = stringResource(id = R.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            itemsIndexed(results) { index, repo ->
                val (backgroundColor, shadowColor, languageColor) = colorSchemeForIndex(index)
                val displayLanguage = repo.language
                    ?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
                val isFavorite = favoriteNames.contains(repo.fullName)
                RepoCard(
                    repoName = repo.fullName,
                    description = repo.description,
                    language = displayLanguage,
                    stars = formatStars(repo.stars),
                    languageColor = languageColor,
                    backgroundColor = backgroundColor,
                    shadowColor = shadowColor,
                    onClick = { onRepositoryClick(repo.fullName, repo.description, repo.stars) },
                    isFavorite = isFavorite,
                    onFavoriteClick = { onToggleFavorite(repo, isFavorite) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RecentSearchesSection(
    recentSearches: List<String>,
    onClearAll: () -> Unit,
    onRemove: (String) -> Unit,
    onSelect: (String) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    if (recentSearches.isEmpty()) {
        EmptyView(
            title = stringResource(id = R.string.search_no_recent),
            description = stringResource(id = R.string.search_no_recent_desc),
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        state = listState
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.search_recent_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PressableTextButton(
                    onClick = onClearAll,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.search_clear_all),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        itemsIndexed(recentSearches) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressable(onClick = { onSelect(item) }, pressedScale = 0.98f)
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                PressableIconButton(onClick = { onRemove(item) }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(id = R.string.cd_remove),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (index < recentSearches.lastIndex) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun formatStars(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fm", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fk", count / 1_000.0)
        else -> count.toString()
    }
}

private fun colorSchemeForIndex(index: Int): Triple<Color, Color, Color> {
    return when (index % 4) {
        0 -> Triple(Color(0xFFF8F7FF), Color(0xFF8B5CF6).copy(alpha = 0.1f), Color(0xFF8B5CF6))
        1 -> Triple(Color(0xFFF0FDF9), Color(0xFF14B8A6).copy(alpha = 0.1f), Color(0xFF14B8A6))
        2 -> Triple(Color(0xFFFFF7ED), Color(0xFFF97316).copy(alpha = 0.1f), Color(0xFFF97316))
        else -> Triple(Color(0xFFFDF2F8), Color(0xFFEC4899).copy(alpha = 0.1f), Color(0xFFEC4899))
    }
}
