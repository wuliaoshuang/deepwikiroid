package com.moxiang.deepwiki.feature.favorites

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moxiang.deepwiki.core.ui.components.EmptyView
import com.moxiang.deepwiki.core.ui.components.RepositoryCard
import com.moxiang.deepwiki.core.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onRepositoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 示例收藏列表
    val favorites = remember {
        listOf(
            FavoriteRepo("jetpack-compose", "Modern toolkit for building native UI", 12400, "Kotlin"),
            FavoriteRepo("okhttp", "Square’s meticulous HTTP client for the JVM, Android, and GraalVM.", 45000, "Java"),
            FavoriteRepo("retrofit", "A type-safe HTTP client for Android and the JVM", 42000, "Java"),
            FavoriteRepo("coil", "Image loading for Android backed by Kotlin Coroutines", 9500, "Kotlin")
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Favorites",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )

                    Surface(
                        color = Color(0x338B5CF6), // Violet with low opacity
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${favorites.size}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (favorites.isEmpty()) {
            EmptyView(
                icon = Icons.Filled.Star,
                title = "暂无收藏内容",
                description = "点击右上角收藏感兴趣的仓库",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(favorites) { index, repo ->
                    val (bgColor, shadowColor) = when (index % 4) {
                        0 -> Color(0xFFF8F7FF) to Color(0xFF8B5CF6) // Violet
                        1 -> Color(0xFFF0FDF9) to Color(0xFF14B8A6) // Teal
                        2 -> Color(0xFFFFF7ED) to Color(0xFFF97316) // Orange
                        else -> Color(0xFFFDF2F8) to Color(0xFFEC4899) // Pink
                    }

                    Card(
                        onClick = { onRepositoryClick(repo.fullName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = shadowColor,
                                spotColor = shadowColor
                            ),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        border = BorderStroke(1.dp, Color(0xFFE4E4E7))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            RepositoryCard(
                                fullName = repo.fullName,
                                description = repo.description,
                                stars = repo.stars,
                                language = repo.language,
                                isFavorite = true,
                                onCardClick = { onRepositoryClick(repo.fullName) },
                                onFavoriteClick = { /* TODO: 取消收藏 */ }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class FavoriteRepo(
    val fullName: String,
    val description: String,
    val stars: Int,
    val language: String?
)
