package com.moxiang.deepwiki.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

val LocalThemeStore = staticCompositionLocalOf<ThemePreferenceStore> {
    error("ThemePreferenceStore not provided")
}
