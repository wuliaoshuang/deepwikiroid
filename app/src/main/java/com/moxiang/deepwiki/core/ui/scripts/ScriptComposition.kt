package com.moxiang.deepwiki.core.ui.scripts

import androidx.compose.runtime.staticCompositionLocalOf

val LocalScriptStore = staticCompositionLocalOf<ScriptPreferenceStore> {
    error("ScriptPreferenceStore not provided")
}

