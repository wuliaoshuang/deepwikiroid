package com.moxiang.deepwiki.core.ui.translation

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.translationDataStore by preferencesDataStore(name = "translation_prefs")

private val ApiKeyKey = stringPreferencesKey("deepseek_api_key")
private val TargetLanguageKey = stringPreferencesKey("target_language")
private val EnabledKey = booleanPreferencesKey("translation_enabled")

/**
 * CompositionLocal for TranslationPreferenceStore
 */
val LocalTranslationStore = staticCompositionLocalOf<TranslationPreferenceStore> {
    error("No TranslationPreferenceStore provided")
}

/**
 * 翻译配置存储
 * 使用 DataStore 持久化翻译相关配置
 */
class TranslationPreferenceStore(private val context: Context) {

    /**
     * DeepSeek API Key Flow
     */
    val apiKeyFlow: Flow<String> = context.translationDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[ApiKeyKey] ?: "" }

    /**
     * 目标语言 Flow
     * 默认: "Chinese"
     */
    val targetLanguageFlow: Flow<String> = context.translationDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[TargetLanguageKey] ?: "Chinese" }

    /**
     * 翻译功能启用状态 Flow
     * 默认: false (需要配置 API Key 后启用)
     */
    val enabledFlow: Flow<Boolean> = context.translationDataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { prefs -> prefs[EnabledKey] ?: false }

    /**
     * 设置 DeepSeek API Key
     */
    suspend fun setApiKey(key: String) {
        context.translationDataStore.edit { prefs ->
            prefs[ApiKeyKey] = key
        }
    }

    /**
     * 设置目标语言
     */
    suspend fun setTargetLanguage(language: String) {
        context.translationDataStore.edit { prefs ->
            prefs[TargetLanguageKey] = language
        }
    }

    /**
     * 设置翻译功能启用状态
     */
    suspend fun setEnabled(enabled: Boolean) {
        context.translationDataStore.edit { prefs ->
            prefs[EnabledKey] = enabled
        }
    }
}
