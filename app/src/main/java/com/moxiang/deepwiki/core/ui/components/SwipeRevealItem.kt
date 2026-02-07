package com.moxiang.deepwiki.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SwipeRevealItem(
    modifier: Modifier = Modifier,
    actionWidth: Dp = 72.dp,
    backgroundColor: Color = Color(0xFFFEE2E2),
    contentShape: Shape,
    actionContent: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }

    var offsetX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = if (dragging) snap() else tween(180),
        label = "swipe-offset"
    )

    Box(modifier = modifier) {

        /* 🔴 Action 区（不参与测量，高度跟随 content） */
        Row(
            modifier = Modifier
                .matchParentSize() // ⭐ 关键：高度 = content 高度
                .background(backgroundColor, contentShape)
                .padding(end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            actionContent()
        }

        /* 🟦 Content 区（唯一的高度来源） */
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragging = true
                        offsetX = (offsetX + delta).coerceIn(-actionWidthPx, 0f)
                    },
                    onDragStopped = {
                        dragging = false
                        offsetX =
                            if (offsetX <= -actionWidthPx / 2f) -actionWidthPx else 0f
                    }
                )
        ) {
            content()
        }
    }
}
