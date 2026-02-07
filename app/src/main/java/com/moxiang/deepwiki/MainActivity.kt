package com.moxiang.deepwiki

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.LocaleListCompat
import com.moxiang.deepwiki.core.ui.locale.AppLanguage
import com.moxiang.deepwiki.core.ui.locale.LanguagePreferenceStore
import com.moxiang.deepwiki.core.ui.locale.LocalLanguageStore
import com.moxiang.deepwiki.core.ui.scripts.LocalScriptStore
import com.moxiang.deepwiki.core.ui.scripts.ScriptPreferenceStore
import com.moxiang.deepwiki.core.ui.theme.DeepWikiTheme
import com.moxiang.deepwiki.core.ui.theme.LocalThemeStore
import com.moxiang.deepwiki.core.ui.theme.ThemeMode
import com.moxiang.deepwiki.core.ui.theme.ThemePreferenceStore
import com.moxiang.deepwiki.core.ui.translation.LocalTranslationStore
import com.moxiang.deepwiki.core.ui.translation.TranslationPreferenceStore

/**
 * MainActivity - Entry point for DeepWiki app
 * Uses the new design system based on Pencil design specifications
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeStore = remember { ThemePreferenceStore(context.applicationContext) }
            val languageStore = remember { LanguagePreferenceStore(context.applicationContext) }
            val scriptStore = remember { ScriptPreferenceStore(context.applicationContext) }
            val translationStore = remember { TranslationPreferenceStore(context.applicationContext) }
            val themeMode by themeStore.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val languageMode by languageStore.languageFlow.collectAsState(initial = AppLanguage.defaultForLocale())
            val darkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            LaunchedEffect(languageMode) {
                val desiredLocales = LocaleListCompat.forLanguageTags(languageMode.localeTag)
                if (AppCompatDelegate.getApplicationLocales() != desiredLocales) {
                    AppCompatDelegate.setApplicationLocales(desiredLocales)
                }
            }

            CompositionLocalProvider(
                LocalThemeStore provides themeStore,
                LocalLanguageStore provides languageStore,
                LocalScriptStore provides scriptStore,
                LocalTranslationStore provides translationStore
            ) {
                DeepWikiTheme(darkTheme = darkTheme) {
                    MainNavigation()
                }
            }
        }
    }
}
