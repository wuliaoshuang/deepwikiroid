package com.moxiang.deepwiki

import android.app.Application
import com.moxiang.deepwiki.data.local.AppDatabase
import com.moxiang.deepwiki.data.repository.FavoriteRepository
import com.moxiang.deepwiki.data.repository.HistoryRepository
import com.moxiang.deepwiki.core.ui.translation.TranslationPreferenceStore

class DeepWikiApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { FavoriteRepository(database.favoriteDao()) }
    val historyRepository by lazy { HistoryRepository(database.historyDao()) }
    val translationStore by lazy { TranslationPreferenceStore(this) }
}
