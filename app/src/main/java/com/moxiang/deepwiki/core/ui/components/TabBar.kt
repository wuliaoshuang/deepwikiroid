package com.moxiang.deepwiki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moxiang.deepwiki.R
import com.moxiang.deepwiki.core.ui.theme.*
import com.moxiang.deepwiki.core.ui.components.pressable

/**
 * Tab Bar Component - Based on Pencil Design
 * Bottom navigation bar with 4 tabs: Home, Favorites, History, Settings
 */
@Composable
fun TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 20.dp, end = 20.dp, bottom = 24.dp, top = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(50.dp),
                    ambientColor = Color.Black.copy(alpha = 0.31f),
                    spotColor = Color.Black.copy(alpha = 0.31f)
                )
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(50.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TabItem(
                    iconAsset = "images/house.svg",
                    label = stringResource(id = R.string.tab_home),
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )

                TabItem(
                    iconAsset = "images/bookmark.svg",
                    label = stringResource(id = R.string.tab_favorites),
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )

                TabItem(
                    iconAsset = "images/history.svg",
                    label = stringResource(id = R.string.tab_history),
                    isSelected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )

                TabItem(
                    iconAsset = "images/settings.svg",
                    label = stringResource(id = R.string.tab_settings),
                    isSelected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    iconAsset: String,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .pressable(onClick = onClick, pressedScale = 0.94f, pressedAlpha = 0.9f)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        AssetSvg(
            assetPath = iconAsset,
            contentDescription = label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
