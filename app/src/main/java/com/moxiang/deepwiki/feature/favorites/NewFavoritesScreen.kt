package com.moxiang.deepwiki.feature.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import com.moxiang.deepwiki.data.local.entity.FavoriteEntity

/**
 * Favorites Screen - Based on Pencil Design
 * Matches specific design node vUQQp
 */
@Composable
fun NewFavoritesScreen(
    onRepositoryClick: (String, String?, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(factory = FavoritesViewModel.Factory)
) {
    val favoriteRepos by viewModel.favorites.collectAsState()
    val navBarsBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val tabBarPadding = 106.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Header with Count Chip
        // Header matches 'favHeader' node: "My Favorites" + Count Chip
        MainHeader(
            title = "My Favorites",
            titleGradient = listOf(
                MaterialTheme.colorScheme.onBackground,
                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
            actions = {
                Surface(
                    color = Color(0x338B5CF6), // Violet with low opacity
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.favorites_repos_count, favoriteRepos.size),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
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

            itemsIndexed(favoriteRepos, key = { _, repo -> repo.repoName }) { index, repo ->
                // Design specifies 4 distinct card styles
                val (bgColor, shadowColor, primaryColor) = when (index % 4) {
                    0 -> Triple(Color(0xFFF8F7FF), Color(0xFF8B5CF6).copy(alpha = 0.1f), Color(0xFF8B5CF6)) // Violet
                    1 -> Triple(Color(0xFFF0FDF9), Color(0xFF14B8A6).copy(alpha = 0.1f), Color(0xFF14B8A6)) // Teal
                    2 -> Triple(Color(0xFFFFF7ED), Color(0xFFF97316).copy(alpha = 0.1f), Color(0xFFF97316)) // Orange
                    else -> Triple(Color(0xFFFDF2F8), Color(0xFFEC4899).copy(alpha = 0.1f), Color(0xFFEC4899)) // Pink
                }

                SwipeRevealItem(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    contentShape = RoundedCornerShape(20.dp),
                    actionContent = {
                        PressableIconButton(
                            onClick = { viewModel.removeFavorite(repo.repoName) },
                            modifier = Modifier.fillMaxHeight()) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.cd_delete),
                                tint = Color(0xFFB91C1C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    content = {
                        FavoriteCard(
                            repo = repo,
                            backgroundColor = bgColor,
                            shadowColor = shadowColor,
                            primaryColor = primaryColor,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onRepositoryClick(repo.repoName, repo.description, repo.stars) }
                        )
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    repo: FavoriteEntity,
    backgroundColor: Color,
    shadowColor: Color,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    // Card Container (vUQQp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp, // visual match for blur:16 y:4
                shape = RoundedCornerShape(20.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFFE4E4E7), // Zinc 200
                shape = RoundedCornerShape(20.dp)
            )
            .pressable(onClick = onClick, pressedScale = 0.985f)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Box (IPWIm)
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = primaryColor,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        // Info Column (0DzVX)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = repo.repoName, // qLX1c
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18181B) // Zinc 900
            )
            Text(
                text = repo.description, // 5AK9p
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF71717A), // Zinc 500
                maxLines = 1
            )
        }

        // Star Icon (QicUd)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = stringResource(id = R.string.cd_favorite),
                tint = Color(0xFFF59E0B), // Amber 500
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = formatStars(repo.stars),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF71717A) // Zinc 500
            )
        }
    }
}

private fun formatStars(count: Int): String {
    return if (count >= 1000) {
        "%.1fk".format(count / 1000f)
    } else {
        count.toString()
    }
}
