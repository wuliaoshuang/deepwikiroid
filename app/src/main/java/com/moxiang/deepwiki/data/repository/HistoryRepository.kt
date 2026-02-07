package com.moxiang.deepwiki.data.repository

import com.moxiang.deepwiki.data.local.dao.HistoryDao
import com.moxiang.deepwiki.data.local.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()

    suspend fun recordHistory(repoName: String, description: String, stars: Int) {
        if (repoName.isBlank()) return
        val entry = HistoryEntity(
            repoName = repoName,
            description = description,
            stars = stars,
            lastVisited = System.currentTimeMillis()
        )
        historyDao.upsertHistory(entry)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun removeHistory(repoName: String) {
        historyDao.deleteByRepo(repoName)
    }
}
