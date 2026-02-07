package com.moxiang.deepwiki.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.AssetSvg
import com.moxiang.deepwiki.core.ui.components.ErrorView
import com.moxiang.deepwiki.core.ui.components.MainHeader
import com.moxiang.deepwiki.core.ui.components.PressableIconButton
import com.moxiang.deepwiki.core.ui.components.PressableTextButton
import com.moxiang.deepwiki.core.ui.components.RepoCard
import com.moxiang.deepwiki.core.ui.components.SearchBar
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.feature.favorites.FavoritesViewModel
import kotlinx.coroutines.launch

/**
 * Home Screen - Based on Pencil Design
 * Main screen showing DeepWiki title, search bar, and trending repositories
 */
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDetail: (String, String?, Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToBrowserTest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel() // 注入 ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoritesViewModel: FavoritesViewModel =
        viewModel(factory = FavoritesViewModel.Factory)
    val favorites by favoritesViewModel.favorites.collectAsState()
    val favoriteNames = remember(favorites) { favorites.map { it.repoName }.toSet() }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarPadding = 106.dp
    val showBackToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > 200
        }
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isLoading,
        onRefresh = { viewModel.loadTrendingRepositories() }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Header (Fixed at top, handles status bar spacing)
        MainHeader(
            title = "DeepWiki",
            subtitle = stringResource(id = R.string.home_subtitle),
            titleGradient = listOf(Purple500, Purple500.copy(0.7f)),
            actions = {
                PressableIconButton(
                    onClick = { onNavigateToBrowserTest() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                ) {
                    AssetSvg(
                        assetPath = "images/folder-plus.svg",
                        contentDescription = stringResource(id = R.string.cd_home_action),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )

        // Content with Pull-to-Refresh
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp + navBarsBottom + tabBarPadding),
                state = listState
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Search Bar
                    Box(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        SearchBar(
                            value = searchQuery,
                            onValueChange = { },
                            placeholder = stringResource(id = R.string.home_search_placeholder),
                            modifier = Modifier
                        )
                        // Transparent clickable overlay
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .pressable(
                                    onClick = { onNavigateToSearch() },
                                    pressedScale = 0.99f,
                                    pressedAlpha = 0.96f
                                )
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Trending Section Header
                    TrendingSectionHeader(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TrendingLoadingPlaceholder()
                        }
                    }
                } else if (uiState.errorDetail != null) {
                    item {
                        val detail = uiState.errorDetail.orEmpty()
                        val message = if (detail.isBlank()) {
                            stringResource(id = R.string.error_load_failed, stringResource(id = R.string.error_unknown))
                        } else {
                            stringResource(id = R.string.error_load_failed, detail)
                        }
                        ErrorView(
                            message = message,
                            onRetry = { viewModel.loadTrendingRepositories() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp)
                        )
                    }
                } else {
                    // Trending Repositories List from API with dynamic colors
                    itemsIndexed(uiState.trendingRepos) { index, repo ->
                        // Select color scheme based on index
                        val colorScheme = when (index % 4) {
                            0 -> Triple(Color(0xFFF8F7FF), Color(0xFF8B5CF6).copy(alpha = 0.1f), Color(0xFF8B5CF6)) // Violet
                            1 -> Triple(Color(0xFFF0FDF9), Color(0xFF14B8A6).copy(alpha = 0.1f), Color(0xFF14B8A6)) // Teal
                            2 -> Triple(Color(0xFFFFF7ED), Color(0xFFF97316).copy(alpha = 0.1f), Color(0xFFF97316)) // Orange
                            else -> Triple(Color(0xFFFDF2F8), Color(0xFFEC4899).copy(alpha = 0.1f), Color(0xFFEC4899)) // Pink
                        }

                        val displayLanguage = repo.language
                            ?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }

                        RepoCard(
                            repoName = repo.fullName,
                            description = repo.description,
                            language = displayLanguage,
                            stars = formatStars(repo.stars),
                            backgroundColor = colorScheme.first,
                            shadowColor = colorScheme.second,
                            languageColor = colorScheme.third,
                            onClick = { onNavigateToDetail(repo.fullName, repo.description, repo.stars) },
                            isFavorite = favoriteNames.contains(repo.fullName),
                            onFavoriteClick = {
                                val isFavorite = favoriteNames.contains(repo.fullName)
                                if (isFavorite) {
                                    favoritesViewModel.removeFavorite(repo.fullName)
                                } else {
                                    favoritesViewModel.addFavorite(repo.fullName, repo.description, repo.stars)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = uiState.isLoading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = Purple500
            )

            androidx.compose.animation.AnimatedVisibility(
                visible = showBackToTop,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 20.dp, bottom = 120.dp)
                        .navigationBarsPadding()
                        .size(44.dp)
                        .pressable(
                            onClick = { scope.launch { listState.animateScrollToItem(0) } },
                            pressedScale = 0.92f,
                            pressedAlpha = 0.9f
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(id = R.string.cd_back_to_top),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * 格式化星数显示 (例如 1234 -> 1.2k)
 */
private fun formatStars(count: Int): String {
    return if (count >= 1000) {
        "%.1fk".format(count / 1000f)
    } else {
        count.toString()
    }
}

@Composable
private fun TrendingSectionHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.home_trending_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        PressableTextButton(onClick = { /* TODO: View all */ }) {
            Text(
                text = stringResource(id = R.string.home_view_all),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Purple500
            )
        }
    }
}

private const val TRENDING_LOADING_LOTTIE_ASSET = "trending_loading.json"

@Composable
private fun TrendingLoadingPlaceholder(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset(TRENDING_LOADING_LOTTIE_ASSET))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(200.dp)
    )
}

// Data class for repository information
private data class RepoData(
    val name: String,
    val description: String,
    val language: String,
    val stars: String,
    val languageColor: Color,
    val backgroundColor: Color,
    val shadowColor: Color
)
