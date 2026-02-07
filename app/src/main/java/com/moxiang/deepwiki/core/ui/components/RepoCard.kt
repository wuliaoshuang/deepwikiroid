package com.moxiang.deepwiki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.components.pressable
import com.moxiang.deepwiki.core.ui.theme.*

/**
 * Repository Card Component - Based on Pencil Design
 * Displays repository information with name, description, language, and stars
 */
@Composable
fun RepoCard(
    repoName: String,
    description: String?,
    language: String?,
    stars: String,
    languageColor: Color,
    backgroundColor: Color,
    shadowColor: Color,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                    Color.Black.copy(alpha = 0.2f)
                } else {
                    shadowColor
                },
                spotColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                    Color.Black.copy(alpha = 0.2f)
                } else {
                    shadowColor
                }
            )
            .background(
                color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                    // Keep colorful cards in dark mode by blending with surface
                    backgroundColor.copy(alpha = 0.2f)
                } else {
                    backgroundColor
                },
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            )
            .pressable(onClick = onClick, pressedScale = 0.985f)
            .padding(16.dp)
    ) {
        val cleanedDescription = description
        ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        val cleanedLanguage = language
        ?.takeIf {
            it.isNotBlank() &&
                !it.equals("null", true) &&
                !it.equals("unknown", true)
        }
        val shouldShowPlaceholders = cleanedDescription == null && cleanedLanguage == null
        val hasDescription = cleanedDescription != null || shouldShowPlaceholders
        val hasLanguage = cleanedLanguage != null || shouldShowPlaceholders
        val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val effectiveBackground = if (isDarkTheme) {
            lerp(MaterialTheme.colorScheme.surface, backgroundColor, 0.4f)
        } else {
            backgroundColor
        }
        val effectiveLanguageColor = if (isDarkTheme) {
            lerp(languageColor, Color.White, 0.2f)
        } else {
            languageColor
        }
        val isDarkCard = effectiveBackground.luminance() < 0.5f
        val titleColor = if (isDarkCard) MaterialTheme.colorScheme.onSurface else Gray900
        val secondaryTextColor = if (isDarkCard) MaterialTheme.colorScheme.onSurfaceVariant else Gray700
        val mutedTextColor = if (isDarkCard) MaterialTheme.colorScheme.onSurfaceVariant else Gray500
        val iconTint = if (isDarkCard) MaterialTheme.colorScheme.onSurfaceVariant else Gray400
        val favoriteTint = if (isFavorite) Purple500 else iconTint
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Repo Name and Stars Count (Moved to top)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = repoName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = titleColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = stringResource(id = R.string.cd_stars),
                    tint = Color(0xFFFFB800), // Golden color for stars
                    modifier = Modifier.size(16.dp)
                )
                    Text(
                        text = stars,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryTextColor
                    )
                }
            }

            if (hasDescription) {
                val displayDescription = cleanedDescription
                    ?: stringResource(id = R.string.repo_no_description)
                Text(
                    text = displayDescription,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = mutedTextColor,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Footer: Language and Bookmark (Favorite button moved to bottom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    // Language Badge (or placeholder)
                Row(
                    modifier = Modifier
                        .background(
                            color = effectiveLanguageColor.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = effectiveLanguageColor,
                                    shape = RoundedCornerShape(5.dp)
                                )
                        )
                    Text(
                        text = cleanedLanguage ?: stringResource(id = R.string.repo_no_language),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = secondaryTextColor
                    )
                }

                // Bookmark Icon (Favorite Button)
                AssetSvg(
                    assetPath = "images/bookmark.svg",
                    contentDescription = stringResource(id = R.string.cd_favorite),
                    tint = favoriteTint,
                    modifier = Modifier
                        .size(20.dp)
                        .pressable(
                            onClick = onFavoriteClick,
                            pressedScale = 0.9f,
                            pressedAlpha = 0.85f,
                            bounded = false
                        )
                )
            }
        }
    }
}
