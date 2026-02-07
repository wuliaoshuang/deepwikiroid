package com.moxiang.deepwiki.core.ui.locale

import androidx.compose.runtime.staticCompositionLocalOf

val LocalLanguageStore = staticCompositionLocalOf<LanguagePreferenceStore> {
    error("LanguagePreferenceStore not provided")
}
