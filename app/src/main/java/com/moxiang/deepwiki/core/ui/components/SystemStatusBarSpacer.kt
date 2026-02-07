package com.moxiang.deepwiki.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * System Status Bar Spacer
 * A reusable spacer component that automatically adjusts to the system status bar height
 * Used to push content below the system status bar
 */
@Composable
fun SystemStatusBarSpacer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.windowInsetsTopHeight(WindowInsets.statusBars)
    )
}
