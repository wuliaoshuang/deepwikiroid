package com.moxiang.deepwiki.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),      // 按钮、标签
    medium = RoundedCornerShape(8.dp),     // 卡片
    large = RoundedCornerShape(16.dp),     // 弹窗、底部抽屉
    extraLarge = RoundedCornerShape(24.dp)
)
