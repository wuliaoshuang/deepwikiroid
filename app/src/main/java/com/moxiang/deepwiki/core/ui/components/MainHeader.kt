package com.moxiang.deepwiki.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.moxiang.deepwiki.R

// 使用本地字体资源（res/font/fugaz_one.ttf）
private val FugazOneFontFamily = FontFamily(
    Font(R.font.fugaz_one)
)

/**
 * Main Screen Header Component
 * 更新：大标题现在使用 Fugaz One 字体
 */
@Composable
fun MainHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleColor: Color = Color.Unspecified,
    titleGradient: List<Color>? = null,
    titleMaxLines: Int = 1,
    leading: (@Composable RowScope.() -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val resolvedTitleColor = if (titleColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onBackground
    } else {
        titleColor
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // System Status Bar Spacer
        Spacer(
            modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
        )

        // Header Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left group: Leading + Title/Subitle
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    Row(content = leading)
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 32.sp, // 微调字号以匹配字体特征
                        fontFamily = FugazOneFontFamily, // 应用 Fugaz One 字体
                        fontWeight = FontWeight.Normal,  // Fugaz One 默认即加粗风格
                        color = if (titleGradient == null) resolvedTitleColor else Color.Unspecified,
                        style = if (titleGradient == null) {
                            TextStyle.Default
                        } else {
                            TextStyle(
                                brush = Brush.linearGradient(colors = titleGradient)
                            )
                        },
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Optional Actions
            if (actions != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}
